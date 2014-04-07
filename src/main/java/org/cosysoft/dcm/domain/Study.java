package org.cosysoft.dcm.domain;

import java.util.List;


//@JsonInclude(Include.ALWAYS)
public class Study {

	public String patientID;
	public String modalitiesInStudy;
	public String studyID;
	public String studyInstanceUID;

	public int seriesCount;

	public List<Series> series;

	@Override
	public String toString() {
		return "Study [patientID=" + patientID + ", studyID=" + studyID
				+ ", studyInstanceUID=" + studyInstanceUID + ", seriesCount="
				+ seriesCount + ", series=" + series + "]";
	}
	
	

}
