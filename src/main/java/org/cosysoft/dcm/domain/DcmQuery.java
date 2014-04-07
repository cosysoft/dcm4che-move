package org.cosysoft.dcm.domain;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class DcmQuery {
	// -c DCM-MCS@172.16.10.227:11112 -m PatientID=822829
	// -L IMAGE -r NumberOfStudyRelatedSeries -r PatientName
	// -r PatientSex -r StudyInstanceUID -r SeriesInstanceUID
	// -r StudyID -r SOPInstanceUID

	private String patId = null;
	private String level = "IMAGE";
	private String suid = null;

	private String date;

	public String getPatId() {
		return patId;
	}

	public void setPatId(String patId) {
		this.patId = patId;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getSuid() {
		return suid;
	}

	public void setSuid(String suid) {
		this.suid = suid;
	}

	/**
	 * 有机会重构为map
	 */
	private List<String> opts = new ArrayList<>();

	private volatile boolean builded = false;

	public void createQuery() {
		if (builded)
			return;

		opts.add("-L");
		opts.add(this.level);

		if (StringUtils.isNotBlank(this.patId)) {
			opts.add("-m");
			opts.add("PatientID=" + this.patId);
		}
		if (StringUtils.isNotBlank(this.suid)) {
			opts.add("-m");
			opts.add("StudyInstanceUID=" + this.suid);
		}
		if (StringUtils.isNotBlank(date)) {
			opts.add("-m");
			opts.add("StudyDate=" + this.date);
		}

		builded = true;
	}

	public List<String> bakedOpts() {
		createQuery();
		return opts;
	}

}
