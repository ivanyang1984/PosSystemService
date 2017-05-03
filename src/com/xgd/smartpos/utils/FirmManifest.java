
package com.xgd.smartpos.utils;

import java.util.List;

public class FirmManifest{
	private List<FwItem> fwItems;
	private String targetTerminal;
	private String vendor;
	private String releaseDate;
	private String firmwareList;
	public String getTargetTerminal() {
		return targetTerminal;
	}
	public void setTargetTerminal(String targetTerminal) {
		this.targetTerminal = targetTerminal;
	}
	public String getVendor() {
		return vendor;
	}
	public void setVendor(String vendor) {
		this.vendor = vendor;
	}
	public String getReleaseDate() {
		return releaseDate;
	}
	public void setReleaseDate(String releaseDate) {
		this.releaseDate = releaseDate;
	}
	public String getFirmwareList() {
		return firmwareList;
	}
	public void setFirmwareList(String firmwareList) {
		this.firmwareList = firmwareList;
	}
	public List<FwItem> getFwItems() {
		return fwItems;
	}
	public void setFwItems(List<FwItem> fwItems) {
		this.fwItems = fwItems;
	}
	
}
