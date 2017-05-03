package com.xgd.smartpos.systemservice;


import com.xgd.smartpos.manager.ICloudService;
import com.xgd.smartpos.nativemanager.IDataService;
import com.xgd.smartpos.utils.TagString;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.WindowManager;
import android.os.ServiceManager;

public class SystemInterfaceService extends Service {
	private final String TAG = TagString.NAME;
	private final int APP_MANAGER = 1;
	private final int SYSTEM_MANAGER = 2;
	private final int DEVICE_MANAGER = 3;
	private IBinder binder;
	private IDataService CppService;
	private Context mContext;
	
	@Override
	public IBinder onBind(Intent arg0) {

		Log.i(TAG, "SystemInterfaceService onBind2 ");
		return mICloudService;
	}

	public void onCreate() {
		binder = ServiceManager.getService("xgddata");
        if (binder == null)
        {
            Log.i(TAG, "can not get xgddata service");
            posStatusDialog("数据服务未运行");
            return;
        }
        CppService = IDataService.Stub.asInterface(binder);
		Log.i(TAG, "SystemInterfaceService onCreate ");
		mContext = this;
	};
	
	
	
	
	ICloudService.Stub mICloudService = new ICloudService.Stub() {
		
		@Override
		public String getServiceSdkVersion() throws RemoteException {
			Log.i(TAG, "getServiceSdkVersion");
			
			return "v1.0.1";
		}
		
		@Override
		public IBinder getManager(int type) throws RemoteException {
			switch (type) {
				case APP_MANAGER:
					return new AppManager(mContext);
				case SYSTEM_MANAGER:
					Log.i(TAG, "SystemManager return ");
					return new SystemManager(mContext, CppService);		
				case DEVICE_MANAGER:
					Log.i(TAG, "return DeviceManager");
					return new DeviceManager();		
				default:
					break;
			}
			
			return null;
		}
	}; 
	
	private void posStatusDialog(String message){
		
		Resources r = Resources.getSystem();  
	    AlertDialog builder = new AlertDialog.Builder(getBaseContext())
	    .setTitle("系统异常")  
	    .setMessage(message)  
	    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {

			} 
		})	
	    .create();  
	    builder.setCancelable(false);
		builder.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);  
		builder.show();
	}
	
	
	
}
