package org.cosysoft.dcm.facade;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.cosysoft.dcm.domain.DcmMoveQuery;
import org.cosysoft.dcm.tool.MoveSCUTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveScuFacade {
	// -b AET1 -c DCM-MCS@172.16.10.227:11112
	// -dest AET1 -m StudyInstanceUID=

	private static final Logger Logger = LoggerFactory
			.getLogger(MoveScuFacade.class);

	DicomConfig config;
	MoveSCUTool moveSCUFacade;

	public void moveByStudyUID(String suid) {
		DcmMoveQuery q = new DcmMoveQuery();
		q.setStudyInstanceUID(suid);
		moveSCUFacade.execute(buildQuery(q));

	}

	private String[] buildQuery(DcmMoveQuery q) {
		List<String> qq = new LinkedList<>();
		qq.add("-c");
		qq.add(config.getRemoteAEUrl());
		qq.add("-b");
		qq.add(config.getLocalAETitle());
		qq.add("-dest");
		qq.add(config.getLocalAETitle());
		qq.addAll(q.bakedOpts());

		String[] qs = qq.toArray(new String[qq.size()]);

		Logger.debug("dicom move \n{}", StringUtils.join(qs, " "));
		return qs;

	}
}
