package org.cosysoft.dcm.service;


public class DicomConfig {

	private String remoteAEUrl;

	private String localAEUrl;

	private String localAETitle;

	private String wadoURI;

	public String getRemoteAEUrl() {
		return remoteAEUrl;
	}

	public void setRemoteAEUrl(String remoteAEUrl) {
		this.remoteAEUrl = remoteAEUrl;
	}

	public String getLocalAEUrl() {
		return localAEUrl;
	}

	public void setLocalAEUrl(String localAEUrl) {
		this.localAEUrl = localAEUrl;
	}

	public String getLocalAETitle() {
		return localAETitle;
	}

	public void setLocalAETitle(String localAETitle) {
		this.localAETitle = localAETitle;
	}

	public String getWadoURI() {
		return wadoURI;
	}

	public void setWadoURI(String wadoURI) {
		this.wadoURI = wadoURI;
	}

}
