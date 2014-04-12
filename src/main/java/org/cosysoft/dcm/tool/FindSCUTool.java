/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is part of dcm4che, an implementation of DICOM(TM) in
 * Java(TM), hosted at https://github.com/gunterze/dcm4che.
 *
 * The Initial Developer of the Original Code is
 * Agfa Healthcare.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * See @authors listed below
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.cosysoft.dcm.tool;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.xml.transform.Templates;
import javax.xml.transform.sax.SAXTransformerFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.cosysoft.dcm.domain.RawStudy;
import org.dcm4che.data.Attributes;
import org.dcm4che.data.Tag;
import org.dcm4che.data.UID;
import org.dcm4che.data.VR;
import org.dcm4che.io.DicomInputStream;
import org.dcm4che.net.ApplicationEntity;
import org.dcm4che.net.Association;
import org.dcm4che.net.Connection;
import org.dcm4che.net.Device;
import org.dcm4che.net.IncompatibleConnectionException;
import org.dcm4che.net.QueryOption;
import org.dcm4che.net.pdu.AAssociateRQ;
import org.dcm4che.net.pdu.ExtendedNegotiation;
import org.dcm4che.net.pdu.PresentationContext;
import org.dcm4che.tool.common.CLIUtils;
import org.dcm4che.util.SafeClose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gunter Zeilinger <gunterze@gmail.com>
 * 
 */

public class FindSCUTool {
	private static Logger logger =  LoggerFactory.getLogger(FindSCUTool.class);
	ExecutorService executorService = Executors.newSingleThreadExecutor();
	ScheduledExecutorService scheduledExecutorService = Executors
			.newSingleThreadScheduledExecutor();

	private static enum InformationModel {
		PatientRoot(UID.PatientRootQueryRetrieveInformationModelFIND, "STUDY"), StudyRoot(
				UID.StudyRootQueryRetrieveInformationModelFIND, "STUDY"), PatientStudyOnly(
				UID.PatientStudyOnlyQueryRetrieveInformationModelFINDRetired,
				"STUDY"), MWL(UID.ModalityWorklistInformationModelFIND, null), UPSPull(
				UID.UnifiedProcedureStepPullSOPClass, null), UPSWatch(
				UID.UnifiedProcedureStepWatchSOPClass, null), HangingProtocol(
				UID.HangingProtocolInformationModelFIND, null), ColorPalette(
				UID.ColorPaletteInformationModelFIND, null);

		final String cuid;
		final String level;

		InformationModel(String cuid, String level) {
			this.cuid = cuid;
			this.level = level;
		}

		public void adjustQueryOptions(EnumSet<QueryOption> queryOptions) {
			if (level == null) {
				queryOptions.add(QueryOption.RELATIONAL);
				queryOptions.add(QueryOption.DATETIME);
			}
		}
	}

	private static ResourceBundle rb = ResourceBundle
			.getBundle("org.dcm4che.tool.findscu.messages");
	private static SAXTransformerFactory saxtf;

	private final Device device = new Device("findscu");
	private final ApplicationEntity ae = new ApplicationEntity("FINDSCU");
	private final Connection conn = new Connection();
	private final Connection remote = new Connection();
	private final AAssociateRQ rq = new AAssociateRQ();
	private int priority;
	private int cancelAfter;
	private InformationModel model;

	private File outDir;
	private DecimalFormat outFileFormat;
	private int[] inFilter;
	private Attributes keys = new Attributes();

	private boolean catOut = false;
	private boolean xml = false;
	private boolean xmlIndent = false;
	private boolean xmlIncludeKeyword = true;
	private boolean xmlIncludeNamespaceDeclaration = false;
	private File xsltFile;
	private Templates xsltTpls;
	private OutputStream out;

	private Association as;
	private AtomicInteger totNumMatches = new AtomicInteger();

	public FindSCUTool() throws IOException {
		device.addConnection(conn);
		device.addApplicationEntity(ae);
		ae.addConnection(conn);
	}

	public final void setPriority(int priority) {
		this.priority = priority;
	}

	public final void setInformationModel(InformationModel model, String[] tss,
			EnumSet<QueryOption> queryOptions) {
		this.model = model;
		rq.addPresentationContext(new PresentationContext(1, model.cuid, tss));
		if (!queryOptions.isEmpty()) {
			model.adjustQueryOptions(queryOptions);
			rq.addExtendedNegotiation(new ExtendedNegotiation(model.cuid,
					QueryOption.toExtendedNegotiationInformation(queryOptions)));
		}
		if (model.level != null)
			addLevel(model.level);
	}

	public void addLevel(String s) {
		keys.setString(Tag.QueryRetrieveLevel, VR.CS, s);
	}

	public final void setCancelAfter(int cancelAfter) {
		this.cancelAfter = cancelAfter;
	}

	public final void setOutputDirectory(File outDir) {
		outDir.mkdirs();
		this.outDir = outDir;
	}

	public final void setOutputFileFormat(String outFileFormat) {
		this.outFileFormat = new DecimalFormat(outFileFormat);
	}

	public final void setXSLT(File xsltFile) {
		this.xsltFile = xsltFile;
	}

	public final void setXML(boolean xml) {
		this.xml = xml;
	}

	public final void setXMLIndent(boolean indent) {
		this.xmlIndent = indent;
	}

	public final void setXMLIncludeKeyword(boolean includeKeyword) {
		this.xmlIncludeKeyword = includeKeyword;
	}

	public final void setXMLIncludeNamespaceDeclaration(
			boolean includeNamespaceDeclaration) {
		this.xmlIncludeNamespaceDeclaration = includeNamespaceDeclaration;
	}

	public final void setConcatenateOutputFiles(boolean catOut) {
		this.catOut = catOut;
	}

	public final void setInputFilter(int[] inFilter) {
		this.inFilter = inFilter;
	}

	private static CommandLine parseComandLine(String[] args)
			throws ParseException {
		Options opts = new Options();
		addServiceClassOptions(opts);
		addKeyOptions(opts);
		addOutputOptions(opts);
		addQueryLevelOption(opts);
		addCancelOption(opts);
		CLIUtils.addConnectOption(opts);
		CLIUtils.addBindOption(opts, "FINDSCU");
		CLIUtils.addAEOptions(opts);
		CLIUtils.addResponseTimeoutOption(opts);
		CLIUtils.addPriorityOption(opts);
		CLIUtils.addCommonOptions(opts);
		return CLIUtils.parseComandLine(args, opts, rb, FindSCUTool.class);
	}

	@SuppressWarnings("static-access")
	private static void addServiceClassOptions(Options opts) {
		opts.addOption(OptionBuilder.hasArg().withArgName("name")
				.withDescription(rb.getString("model")).create("M"));
		CLIUtils.addTransferSyntaxOptions(opts);
		opts.addOption(null, "relational", false, rb.getString("relational"));
		opts.addOption(null, "datetime", false, rb.getString("datetime"));
		opts.addOption(null, "fuzzy", false, rb.getString("fuzzy"));
		opts.addOption(null, "timezone", false, rb.getString("timezone"));
	}

	@SuppressWarnings("static-access")
	private static void addQueryLevelOption(Options opts) {
		opts.addOption(OptionBuilder.hasArg()
				.withArgName("PATIENT|STUDY|SERIES|IMAGE")
				.withDescription(rb.getString("level")).create("L"));
	}

	@SuppressWarnings("static-access")
	private static void addCancelOption(Options opts) {
		opts.addOption(OptionBuilder.withLongOpt("cancel").hasArg()
				.withArgName("num-matches")
				.withDescription(rb.getString("cancel")).create());
	}

	@SuppressWarnings("static-access")
	private static void addKeyOptions(Options opts) {
		opts.addOption(OptionBuilder.hasArgs().withArgName("[seq/]attr=value")
				.withValueSeparator('=').withDescription(rb.getString("match"))
				.create("m"));
		opts.addOption(OptionBuilder.hasArgs().withArgName("[seq/]attr")
				.withDescription(rb.getString("return")).create("r"));
		opts.addOption(OptionBuilder.hasArgs().withArgName("attr")
				.withDescription(rb.getString("in-attr")).create("i"));
	}

	@SuppressWarnings("static-access")
	private static void addOutputOptions(Options opts) {
		opts.addOption(OptionBuilder.withLongOpt("out-dir").hasArg()
				.withArgName("directory")
				.withDescription(rb.getString("out-dir")).create());
		opts.addOption(OptionBuilder.withLongOpt("out-file").hasArg()
				.withArgName("name").withDescription(rb.getString("out-file"))
				.create());
		opts.addOption("X", "xml", false, rb.getString("xml"));
		opts.addOption(OptionBuilder.withLongOpt("xsl").hasArg()
				.withArgName("xsl-file").withDescription(rb.getString("xsl"))
				.create("x"));
		opts.addOption("I", "indent", false, rb.getString("indent"));
		opts.addOption("K", "no-keyword", false, rb.getString("no-keyword"));
		opts.addOption(null, "xmlns", false, rb.getString("xmlns"));
		opts.addOption(null, "out-cat", false, rb.getString("out-cat"));
	}

	public List<RawStudy> exeucte(String[] args) {
		List<RawStudy> studies = new ArrayList<>();
		try {
			CommandLine cl = parseComandLine(args);
			FindSCUTool main = new FindSCUTool();
			CLIUtils.configureConnect(main.remote, main.rq, cl);
			CLIUtils.configureBind(main.conn, main.ae, cl);
			CLIUtils.configure(main.conn, cl);
			main.remote.setTlsProtocols(main.conn.getTlsProtocols());
			main.remote.setTlsCipherSuites(main.conn.getTlsCipherSuites());
			configureServiceClass(main, cl);
			configureKeys(main, cl);
			configureOutput(main, cl);
			configureCancel(main, cl);
			main.setPriority(CLIUtils.priorityOf(cl));

			main.device.setExecutor(executorService);
			main.device.setScheduledExecutor(scheduledExecutorService);
			try {
				main.open();
				SyncFutureDimseRSP rsp = main.query();

				while (!rsp.isFinished()) {
				}
				List<RawStudy> s = rsp.getRawStudy();
				return s;
			} finally {
				main.close();
			}
		} catch (ParseException e) {
			logger.error("{}",e);
			logger.debug(rb.getString("try"));
		} catch (Exception e) {
			logger.error("{}",e);
		}

		return studies;
	}

	private static EnumSet<QueryOption> queryOptionsOf(FindSCUTool main,
			CommandLine cl) {
		EnumSet<QueryOption> queryOptions = EnumSet.noneOf(QueryOption.class);
		if (cl.hasOption("relational"))
			queryOptions.add(QueryOption.RELATIONAL);
		if (cl.hasOption("datetime"))
			queryOptions.add(QueryOption.DATETIME);
		if (cl.hasOption("fuzzy"))
			queryOptions.add(QueryOption.FUZZY);
		if (cl.hasOption("timezone"))
			queryOptions.add(QueryOption.TIMEZONE);
		return queryOptions;
	}

	private static void configureOutput(FindSCUTool main, CommandLine cl) {
		if (cl.hasOption("out-dir"))
			main.setOutputDirectory(new File(cl.getOptionValue("out-dir")));
		main.setOutputFileFormat(cl.getOptionValue("out-file", "000.dcm"));
		main.setConcatenateOutputFiles(cl.hasOption("out-cat"));
		main.setXML(cl.hasOption("X"));
		if (cl.hasOption("x")) {
			main.setXML(true);
			main.setXSLT(new File(cl.getOptionValue("x")));
		}
		main.setXMLIndent(cl.hasOption("I"));
		main.setXMLIncludeKeyword(!cl.hasOption("K"));
		main.setXMLIncludeNamespaceDeclaration(cl.hasOption("xmlns"));
	}

	private static void configureCancel(FindSCUTool main, CommandLine cl) {
		if (cl.hasOption("cancel"))
			main.setCancelAfter(Integer.parseInt(cl.getOptionValue("cancel")));
	}

	private static void configureKeys(FindSCUTool main, CommandLine cl) {
		CLIUtils.addEmptyAttributes(main.keys, cl.getOptionValues("r"));
		CLIUtils.addAttributes(main.keys, cl.getOptionValues("m"));
		if (cl.hasOption("L"))
			main.addLevel(cl.getOptionValue("L"));
		if (cl.hasOption("i"))
			main.setInputFilter(CLIUtils.toTags(cl.getOptionValues("i")));
	}

	private static void configureServiceClass(FindSCUTool main, CommandLine cl)
			throws ParseException {
		main.setInformationModel(informationModelOf(cl),
				CLIUtils.transferSyntaxesOf(cl), queryOptionsOf(main, cl));
	}

	private static InformationModel informationModelOf(CommandLine cl)
			throws ParseException {
		try {
			return cl.hasOption("M") ? InformationModel.valueOf(cl
					.getOptionValue("M")) : InformationModel.StudyRoot;
		} catch (IllegalArgumentException e) {
			throw new ParseException(MessageFormat.format(
					rb.getString("invalid-model-name"), cl.getOptionValue("M")));
		}
	}

	public void open() throws IOException, InterruptedException,
			IncompatibleConnectionException, GeneralSecurityException {
		as = ae.connect(conn, remote, rq);
	}

	public void close() throws IOException, InterruptedException {
		if (as != null && as.isReadyForDataTransfer()) {
			as.waitForOutstandingRSP();
			as.release();
		}
		SafeClose.close(out);
		out = null;
	}

	public void query(File f) throws IOException, InterruptedException {
		Attributes attrs;
		DicomInputStream dis = null;
		try {
			attrs = new DicomInputStream(f).readDataset(-1, -1);
			if (inFilter != null) {
				attrs = new Attributes(inFilter.length + 1);
				attrs.addSelected(attrs, inFilter);
			}
		} finally {
			SafeClose.close(dis);
		}
		attrs.addAll(keys);
		query(attrs);
	}

	public SyncFutureDimseRSP query() throws IOException, InterruptedException {
		return query(keys);
	}

	private SyncFutureDimseRSP query(Attributes keys) throws IOException,
			InterruptedException {
		SyncFutureDimseRSP dimseRSP = new SyncFutureDimseRSP(as.nextMessageID());
		as.cfind(model.cuid, priority, keys, null, dimseRSP);
		return dimseRSP;
	}

	@PreDestroy
	public void destroy() {
		executorService.shutdown();
		scheduledExecutorService.shutdown();
	}
}
