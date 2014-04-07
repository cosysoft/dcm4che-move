package org.cosysoft.dcm.domain;

import java.util.List;

public class Series {

	public String studyInstanceUID;
	public String seriesInstanceUID;
	public int imageCount;
	public List<Image> images;

	@Override
	public String toString() {
		return "Series [seriesInstanceUID=" + seriesInstanceUID
				+ ", imageCount=" + imageCount + ", images=" + images + "]";
	}
	
}