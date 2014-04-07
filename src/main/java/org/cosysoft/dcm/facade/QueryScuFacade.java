package org.cosysoft.dcm.facade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.cosysoft.dcm.domain.DcmQuery;
import org.cosysoft.dcm.domain.Image;
import org.cosysoft.dcm.domain.RawStudy;
import org.cosysoft.dcm.domain.Study;
import org.cosysoft.dcm.tool.FindSCU;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryScuFacade {
	private static final List<String> findOpts = Arrays.asList("-r",
			"PatientName", "-r", "StudyInstanceUID", "-r", "StudyID", "-r",
			"StudyDate", "-r", "NumberOfPatientRelatedStudies", "-r",
			"NumberOfStudyRelatedSeries", "-r",
			"NumberOfSeriesRelatedInstances", "-r", "SeriesInstanceUID", "-r",
			"SOPInstanceUID", "-r", "PatientID", "-r", "ModalitiesInStudy");

	private Logger logger = LoggerFactory.getLogger(QueryScuFacade.class);
	private FindSCU findSCUFacade;
	private DicomConfig config;

	public List<Study> getStudyByPatId(String patientID) {

		DcmQuery query = new DcmQuery();
		query.setPatId(patientID);
		List<Study> s = this.getStudyByQuery(query);
		return s;
	}

	public List<RawStudy> getStudyOnly(DcmQuery query) {
		List<RawStudy> s = findSCUFacade.exeucte(buildLocalDcmQuery(query));
		return s;
	}

	private List<Study> getStudyByQuery(DcmQuery q) {
		List<Study> sts = new ArrayList<>();
		List<RawStudy> rsts = findSCUFacade.exeucte(buildLocalDcmQuery(q));
		sts = RawStudy.toStudy(rsts);
		return sts;
	}

	private String[] buildLocalDcmQuery(DcmQuery q) {

		List<String> qq = new ArrayList<>();
		// qq.add("-b");
		// qq.add(config.getLocalAETitle());
		qq.add("-c");
		qq.add(config.getLocalAEUrl());

		qq.addAll(q.bakedOpts());
		qq.addAll(findOpts);
		String[] qs = qq.toArray(new String[qq.size()]);

		logger.info("dicom 查询命令 \n{}", StringUtils.join(qs, " "));
		return qs;

	}

	@PostConstruct
	public void init() {
		Image.url = config.getWadoURI();
	}
}
