package org.cosysoft.dcm.domain;


public class Image {

	/**
	 * It's danger!
	 * @see QueryScuService.init()
	 */
	public static String url = "http://192.168.2.122:8090/wado?requestType=WADO";

	public String studyInstanceUID;
	public String seriesInstanceUID;
	public String sopInstanceUID;

	public String getImageUrl() {
		return url + "&studyUID=" + studyInstanceUID + "&seriesUID="
				+ seriesInstanceUID + "&objectUID=" + sopInstanceUID;
	}
}
