package com.xgd.smartpos.systemservice;

import java.io.File;
import java.lang.reflect.Method;

import com.xgd.smartpos.manager.app.IAppDeleteObserver;
import com.xgd.smartpos.manager.app.IAppInstallObserver;
import com.xgd.smartpos.manager.app.IAppManager;
import com.xgd.smartpos.utils.TagString;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException; 
import android.util.Log;
import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstallObserver2;  
import android.content.pm.IPackageManager;  
import android.content.pm.PackageManager;
import android.content.pm.VerificationParams; 
import android.os.ServiceManager;

public class AppManager extends  IAppManager.Stub{
	private final String TAG = TagString.NAME;
	private IAppInstallObserver mInstallObserver;
	private IAppDeleteObserver mIAppDeleteObserver;
	private Context mContext;
	
	public AppManager(Context context) {
		mContext = context;
	}

	@Override
	public void installApp(String apkPath, IAppInstallObserver observer,
			String installerPackageName) throws RemoteException {

		Log.i(TAG, "apkPath:" + apkPath);
		mInstallObserver = observer;
		
		String fileName = apkPath;
		File file = new File(fileName);
		try {
			if(file.exists() == false){
				return;
			}
			IBinder binder = ServiceManager.getService("package");
	        if (binder == null){
	            Log.i(TAG, "can not get package service");
	            return;
	        }
	        IPackageManager ipm = IPackageManager.Stub.asInterface(binder);
			VerificationParams verificationParams = new VerificationParams(null, null, null, VerificationParams.NO_UID, null);
//			int ret = PackageManager.INSTALL_REPLACE_EXISTING;
			ipm.installPackage(apkPath, new PackageInstallObserver(), 0, installerPackageName, verificationParams, null);
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "installApp:" + e.toString());
		}



	}

	@Override
	public void uninstallApp(String packageName, IAppDeleteObserver observer)
			throws RemoteException {
		mIAppDeleteObserver = observer;
		try {
			IBinder binder = ServiceManager.getService("package");
	        if (binder == null){
	            Log.i(TAG, "can not get package service");
	            return;
	        }
	        IPackageManager ipm = IPackageManager.Stub.asInterface(binder);
			ipm.deletePackage(packageName, new PackageDeleteObserver2(), 0, 0);
		} catch (Exception e) {
			e.printStackTrace();
			Log.i(TAG, "installApp:" + e.toString());
		}

	}
	// 用于显示结果    
	class PackageInstallObserver extends IPackageInstallObserver2.Stub {
		@Override
		public void onUserActionRequired(Intent intent) throws RemoteException {
		}
		@Override
		public void onPackageInstalled(String basePackageName, int returnCode, String msg, Bundle extras) throws RemoteException {
			Log.i(TAG, "basePackageName:" + basePackageName);
			Log.i(TAG, "returnCode:" + returnCode);
			Log.i(TAG, "msg:" + msg);
			mInstallObserver.onInstallFinished(basePackageName, returnCode, msg);
		}; 
	}

	class PackageDeleteObserver2 extends IPackageDeleteObserver2.Stub{

		@Override
		public void onUserActionRequired(Intent intent) throws RemoteException {
			
		}

		@Override
		public void onPackageDeleted(String packageName, int returnCode,
				String msg) throws RemoteException {
			mIAppDeleteObserver.onDeleteFinished(packageName, returnCode, msg);
		}
		
	}

}


