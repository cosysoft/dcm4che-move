package org.cosysoft.dcm.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class DcmMoveQuery {
	// -b AET1 -c DCM-MCS@172.16.10.227:11112
	// -dest AET1 -m StudyInstanceUID=

	private String studyInstanceUID;

	private String patientID;

	private List<String> opts = new ArrayList<>();

	private volatile boolean builded = false;

	public void createQuery() {
		if (builded)
			return;
		if (StringUtils.isNotBlank(this.studyInstanceUID)) {
			opts.add("-m");
			opts.add("StudyInstanceUID=" + this.studyInstanceUID);
		}
		if (StringUtils.isNotBlank(this.patientID)) {
			opts.add("-m");
			opts.add("PatientID=" + this.patientID);
		}

		builded = true;
	}

	public List<String> bakedOpts() {
		createQuery();
		return opts;
	}

	public String getStudyInstanceUID() {
		return studyInstanceUID;
	}

	public void setStudyInstanceUID(String studyInstanceUID) {
		this.studyInstanceUID = studyInstanceUID;
	}

	public String getPatientID() {
		return patientID;
	}

	public void setPatientID(String patientID) {
		this.patientID = patientID;
	}

}
