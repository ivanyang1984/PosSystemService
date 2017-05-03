package com.xgd.smartpos.systemservice;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;

import com.xgd.smartpos.manager.device.IDeviceManager;
import com.xgd.smartpos.utils.TagString;

//import android.os.SystemProperties;

public class DeviceManager extends IDeviceManager.Stub{
	private final String TAG = TagString.NAME;
    private static final String VENDOR = "vendor";
    private static final String MODEL = "model";
    private static final String SN = "sn";
    private static final String OS_VERSION = "os_version";
    private static final String SERVICE_APP_VERSION = "service_app_version";
    
	@Override
	public Bundle getDeviceInfo() throws RemoteException {
		Bundle mBundle = new Bundle(); 

		Log.i(TAG, "DeviceManager getDeviceInfo");
		String manufacturer = android.os.Build.MANUFACTURER;
		if(manufacturer.equals("unknown")){
			manufacturer = "XGD";
		}
		mBundle.putString(VENDOR, manufacturer);
		mBundle.putString(MODEL, android.os.Build.MODEL);
		
		String str = android.os.SystemProperties.get("ro.ums.manufacturer.info");
		String[]  strs=str.split("\\|");
		String sn= strs[2];	
		mBundle.putString(SN, sn);
		
		mBundle.putString(OS_VERSION, android.os.Build.VERSION.RELEASE);
		
		mBundle.putString(SERVICE_APP_VERSION, "1.0.0");
		
		return mBundle;
	}

	@Override
	public String getHardWireVersion() throws RemoteException {
		return "Q-4-01.02";
	}

	@Override
	public String getRomVersion() throws RemoteException {
		String PROPETY_FIRMWARE_VERSION = "ro.product.firmware";
		String PROPETY_PRODUCT_MODEL = "ro.product.model";
	  	String PROPETY_CUSTOM_VERSION = "ro.custom.version";
	  	
		return SystemProperties.get(PROPETY_FIRMWARE_VERSION, "v1.0.0") + "_" 
	               + SystemProperties.get(PROPETY_PRODUCT_MODEL, "N5") + SystemProperties.get(PROPETY_CUSTOM_VERSION, "000001");
	}

	@Override
	public String getAndroidKernelVersion() throws RemoteException {
        String kernelVersion = "";  
        InputStream inputStream = null;  
        try {  
            inputStream = new FileInputStream("/proc/version");  
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
            return kernelVersion;  
        }  
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 8 * 1024);  
        String info = "";  
        String line = "";  
        try {  
            while ((line = bufferedReader.readLine()) != null) {  
                info += line;  
            }  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            try {  
                bufferedReader.close();  
                inputStream.close();  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
      
        try {  
            if (info != "") {  
                final String keyword = "version ";  
                int index = info.indexOf(keyword);  
                line = info.substring(index + keyword.length());  
                index = line.indexOf(" ");  
                kernelVersion = line.substring(0, index);  
            }  
        } catch (IndexOutOfBoundsException e) {  
            e.printStackTrace();  
        }  
      
        return kernelVersion; 
	}

}
