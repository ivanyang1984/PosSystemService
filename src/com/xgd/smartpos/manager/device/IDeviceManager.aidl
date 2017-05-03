package com.xgd.smartpos.manager.device;
import android.os.Bundle;



interface IDeviceManager {
	Bundle getDeviceInfo();
	String getHardWireVersion();
	String getRomVersion();
	String getAndroidKernelVersion();
}
