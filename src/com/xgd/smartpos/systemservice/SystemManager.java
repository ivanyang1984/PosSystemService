package com.xgd.smartpos.systemservice;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.security.PublicKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.AlarmManager;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.IBinder;
import android.os.RemoteCallbackList;   
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.StatFs;
import android.util.Log;
import android.util.Xml;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xgd.smartpos.lakala.ShellMonitor;
import com.xgd.smartpos.manager.app.IAppInstallObserver;
import com.xgd.smartpos.manager.system.IBackupObserver;
import com.xgd.smartpos.manager.system.IRestoreObserver;
import com.xgd.smartpos.manager.system.ISystemManager;
import com.xgd.smartpos.manager.system.ITaskCallback;
import com.xgd.smartpos.nativemanager.IDataService;
import com.xgd.smartpos.utils.MD5;
import com.xgd.smartpos.utils.OtaUpgradeUtils;
import com.xgd.smartpos.utils.TagString;
import com.xgd.smartpos.utils.FirmManifest;
import com.xgd.smartpos.utils.FwItem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;


public class SystemManager extends  ISystemManager.Stub{
	private static final int UPDATE_OS = 1;
	
	private static final int UPDATE_FIRMWARE_OS = 0;
	private static final int UPDATE_FIRMWARE_RES = 1;
	
	private static final int UPDATE_FW_STATE_SUCCESS = 0;
	private static final int UPDATE_FW_STATE_DOING = 1;
	private static final int UPDATE_FW_STATE_FAILED = 2;
	private static final int UPDATE_FW_STATE_SIGN_FAILED = 3;
	private static final int UPDATE_FW_STATE_UNSUPPORT = 4;
	
	private static final String UPDATE_FW_NEED_RESTORE = "need_restore";
	private static final String UPDATE_FW_STORE_ITEMS = "update_items";
	
	private Context mContext;
	private final String TAG = TagString.NAME;
	private IDataService CppService;
	private final String BACKUPPATH = "/sdcard/backup";
	private final String STORAGE_PATH = "/sdcard/lkl/";
	private final String UNZIP_PATH = "/sdcard/lkl/tmp/";
	private int mLevel;
	private int mStatus;
	//0 ： 更新成功 ； 1 ： 正在更新； 2 ：更新失败 3：验签不过 4：本终端不⽀持该固件包更新
	private int mFirmUpdateStatus = 0;
	private String mFirmPackageName = null;
	
	private List<FwItem> mFirmInfoItems = null;
	private final String LKLPUBLICKEY = "/system/poscert/lakala.key";
	private String[] mOpenRootCmds = new String[]{"ifconfig", "iptables", "ip6tables", "ping", "ping6", "route", "netcfg", "ip"};
	private int year = 0, month = 0, day = 0, hour = 0, minute = 0;
	
	public SystemManager(Context context, IDataService cppService) {
		mContext = context;
		CppService = cppService;
		 context.getApplicationContext().registerReceiver(mBatInfoReceiver,   
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); 
	}

	@Override
	public void updateSystem(String filePath, int type) throws RemoteException {
		
		if(type == UPDATE_OS){
			Log.i(TAG, "update os");
			OtaUpgradeUtils mUpdateUtils = new OtaUpgradeUtils(mContext);
			mUpdateUtils.upgradeFromOta(filePath, CppService);
		}
		
	}

	@Override
	public String getStoragePath()throws RemoteException {
		return STORAGE_PATH;
	}
	
	@Override
	public	int getUpdateFirmwareState(String updateId)throws RemoteException{
		return mFirmUpdateStatus;
	}
	
	@Override
	public String updateFirmware(int type,String packageName) throws RemoteException {
		if(packageName == null){
			return "PACKAGENAME_NULL";
		}
		
		mFirmPackageName = packageName;
		
		//判断是否有上一次未完成的任务
		String needRestore = getPreference(UPDATE_FW_NEED_RESTORE);
		//恢复上一次的升级
		if(needRestore.equals("true")){
			mFirmInfoItems = restoreFirmInfoItems(UPDATE_FW_STORE_ITEMS);
			mFirmUpdateStatus = 1;
			installOneApp();
			return "UPDATE_FW_STATE_DOING";
		}

		//判断文件是否存在
		File firmFile = new File(STORAGE_PATH + packageName);
		if(!firmFile.exists()){
			mFirmUpdateStatus = UPDATE_FW_STATE_FAILED;
			return "ERR_FILE_NOT_FOUND";
		}

        //删除上一次的解压文件
        deleteFile(UNZIP_PATH);
		
		/******1. 解压固件包*********/
		unZipFile(firmFile,UNZIP_PATH);
		
		/*********2.解析配置文件*********/
		FirmManifest firmInfo = null;
		File f = new File(UNZIP_PATH + "updatemanifest.xml");
		if(!f.exists()){
			mFirmUpdateStatus = UPDATE_FW_STATE_FAILED;
			return "ERR_UPDATE_MANIFEST_NOT_FOUND";
		}
		try {
			InputStream in = new FileInputStream(f);
			firmInfo = parseXML(in);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			mFirmUpdateStatus = UPDATE_FW_STATE_FAILED;
			return "ERR_PARSE_MANIFEST_FileNotFoundException";
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			mFirmUpdateStatus = UPDATE_FW_STATE_FAILED;
			return "ERR_PARSE_MANIFEST_XmlPullParserException";
		} catch (IOException e) {
			e.printStackTrace();
			mFirmUpdateStatus = UPDATE_FW_STATE_FAILED;
			return "ERR_PARSE_MANIFEST_IOException";
		}
		
		if(firmInfo.getFwItems().size() < 1){
			mFirmUpdateStatus = UPDATE_FW_STATE_FAILED;
			return "ERR_MANIFEST_FILE";
		}
		
		//TODO 校验 TargetTerminal Vendor 出错返回   UPDATE_FW_STATE_UNSUPPORT 状态
		
		//disable it temper
        /*
		if(!checkFileEMD5(firmInfo)){
			mFirmUpdateStatus = UPDATE_FW_STATE_SIGN_FAILED;
			return "ERR_CHECK_EMD5";
		}*/
		
		mFirmInfoItems = firmInfo.getFwItems();
		
		
		if(type == UPDATE_FIRMWARE_OS){
			/******系统升级*******/
			Log.i(TAG, "update firm os");
			deleteFile(STORAGE_PATH + packageName);
			OtaUpgradeUtils mUpdateUtils = new OtaUpgradeUtils(mContext);
			mUpdateUtils.upgradeFromOta(UNZIP_PATH + firmInfo.getFwItems().get(0).getName(), CppService);
			return packageName;
			
		}else if(type == UPDATE_FIRMWARE_RES){
			/******资源(APP)升级*******/
			//正在更新
			Log.d("[gx]", "UPDATE_FIRMWARE_RES:" + UPDATE_FIRMWARE_RES);
			mFirmUpdateStatus = 1;
			installOneApp();
		}
		return packageName;
	}

	@Override
    public void registerCallback(ITaskCallback cb) throws RemoteException {  
        if (cb != null) {   
            mCallbacks.register(cb);  
        }  
    }  

	@Override
    public void unregisterCallback(ITaskCallback cb) throws RemoteException {  
        if(cb != null) {  
            mCallbacks.unregister(cb);  
        }  
    }  


    public static void callback(int val) {   
        final int N = mCallbacks.beginBroadcast();  
        for (int i=0; i<N; i++) {   
            try {  
                mCallbacks.getBroadcastItem(i).actionPerformed(val);   
            }  
            catch (RemoteException e) {   
                // The RemoteCallbackList will take care of removing   
                // the dead object for us.     
            }  
        }  
        mCallbacks.finishBroadcast();  
    }  

	@Override
	public void reboot() throws RemoteException {
		Log.i(TAG, "SystemManager reboot");
		PowerManager pManager=(PowerManager) mContext.getSystemService(Context.POWER_SERVICE);  
		pManager.reboot("");
	}

	@Override
	public void backupByPackage(List<String> packageName,
			IBackupObserver observer) throws RemoteException {
		Log.i(TAG, "backupByPackage");
		
        int cnt = 0;
        int ret;
        
        String tmppath = BACKUPPATH+"/tmp/";

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String filePath = BACKUPPATH+"/backup_" + df.format(new Date())+".zip";
        
        File path = new File(BACKUPPATH);
        if(path.exists() == false){
        	path.mkdir();
        }
        Log.i(TAG, "filePath: " + filePath);
        if(getPartitionAvailSizeM(BACKUPPATH) < 50){
        	ret = -1;
        	observer.onBackupFinished(ret, "ERR: not enough space", filePath);
        	return ;
        }
        

        CppService.shellcmd("rm -f " + path);
        CppService.shellcmd("rm -rf " + tmppath);
        CppService.shellcmd("mkdir " + tmppath);
        
        for(String name : packageName){
        	
//        	if(getUid(name) > 0){
        	if(new File("/data/data/"+name).exists() == true){
        		Log.i(TAG, name + ": AppBackup ok");
	        	CppService.shellcmd("cp -rf /data/data/" + name + " " + tmppath);
	        	CppService.shellcmd("rm -rf " + tmppath +name+"/lib " + tmppath +name+"/cache ");
	        	cnt++;
        	}else{
        		Log.i(TAG, name + ": AppBackup err");
        	}

        }
        if(cnt == 0){
        	Log.i(TAG, "ERR: Can not find packageName");
        	ret = -2;
        	observer.onBackupFinished(ret, "ERR: Can not find packageName", filePath);
        	return ;
        }
        
		CppService.shellcmd("busybox tar cf "+ filePath + " " + tmppath);
		CppService.shellcmd("rm -rf  "+ tmppath);
		
		
		observer.onBackupFinished(0, "Backup successful", filePath);
	}

	@Override
	public void restore(String path, IRestoreObserver observer)
			throws RemoteException {
		
		String tmppath = BACKUPPATH+"/tmp/";
        int uid;
        int cnt = 0;
        
        if(path == null){
        	observer.onRestoreFinished(-4, "ERR: on a null object reference");
        	return ;
        }
        
        if(new File(path).exists() == false){
        	observer.onRestoreFinished(-3, "ERR: Backup package not found");
        	return ;
        }
        if(getPartitionAvailSizeM(BACKUPPATH) < 50){
        	observer.onRestoreFinished(-1, "ERR: not enough space");
        	return ;
        }
        CppService.shellcmd("rm -rf " + tmppath);
        CppService.shellcmd("busybox tar xf " + path + " -C /");
        CppService.shellcmd("chmod 755 " + tmppath);
        

        
        File backuptmp =  new File(tmppath);
        File[] files = backuptmp.listFiles();

        for(File file : files){
//        	Log.i(TAG, "mDataBackup = " + file.getName() + ":"+ getUid(file.getName()));
        	uid = getUid(file.getName());
        	if(uid > 0){
        		CppService.shellcmd("cp -rf " + file.getAbsolutePath() + " /data/data/");
        		CppService.shellcmd("busybox chown -R " + uid + ":" + uid + " /data/data/" + file.getName());
        		CppService.shellcmd("busybox chmod 755 -R " + " /data/data/" + file.getName());
        		cnt++;
        	}else{
        		Log.i(TAG, "Without this process:"+ file.getName());
        	}
        }
        
        if(cnt == 0){
        	Log.i(TAG, "ERR: Can not find packageName");
        	observer.onRestoreFinished(-2, "ERR: The restore application does not exist");
        	return ;
        }
        
        CppService.shellcmd("rm -rf " + tmppath);
        observer.onRestoreFinished(0, "restore successful");
	}
	
	private int getUid(String packageName){
		try {
            PackageManager pm = mContext.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(packageName, PackageManager.GET_ACTIVITIES);
            return ai.uid;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
		return -1;
	}
	
	/**
	 * @param partition
	 * @return
	 */
	private static int getPartitionAvailSizeM(String partition){
    	File file = new File(partition);
        StatFs sf = new StatFs(file.getPath());  
        
		long blockSize = sf.getBlockSize();
//        long blockCount = sf.getBlockCount();
        long availCount = sf.getAvailableBlocks();

        return (int) (availCount*blockSize/1024/1024);
        
    }
    static  final RemoteCallbackList <ITaskCallback>mCallbacks = new RemoteCallbackList <ITaskCallback>();  
	
	@Override
	public boolean executeCmd(String cmd) throws RemoteException {
		Log.i(TAG, "executeCmd***:" + cmd);
		Runtime runtime = Runtime.getRuntime();    
        Process proc;
		try {
			proc = runtime.exec(cmd);
//			if (proc.waitFor() != 0) {  
//                Log.i(TAG, "err: executeCmd exit value = " + proc.exitValue());
//            }
			return true;
		} catch (IOException e1) {
			e1.printStackTrace();
		} 
		return false;
	}

	@Override
	public boolean executeRootCMD(String packageName, String rootkey, String authToken,
			String cmdParams) throws RemoteException {
		Log.i(TAG, "executeRootCMD***" + packageName);
		Log.i(TAG, "rootkey***" + rootkey);
		Log.i(TAG, "authToken***" + authToken);
		Log.i(TAG, "cmdParams***" + cmdParams);
		boolean authStatus = false;
		String[] cmdparams = cmdParams.split(" ");
		for(int i = 0; i < mOpenRootCmds.length; i++){
			if(cmdparams[0].equals(mOpenRootCmds[i])){
				authStatus = true;
				break;
			}
		}
		if(authStatus == false){
			Log.i(TAG, "Not open root cmd: " + cmdparams[0]);
			return false;
		}
		ShellMonitor shellmonitor = new ShellMonitor();
		PublicKey publicKey = shellmonitor.getLKLPublicKey(LKLPUBLICKEY);
		String decryptedRootkey = shellmonitor.publicKeyDecrypt(publicKey, rootkey);
		if(decryptedRootkey == null){
			Log.i(TAG, "Rootkey decryption failure");
			return false;
		}
		
		String[] strs = decryptedRootkey.split("#");
		String MD5Rootkey = strs[0];
		String decryptedAuthToken = strs[1];
		Log.i(TAG, "decryptedAuthToken***" + decryptedAuthToken);
		if(decryptedAuthToken.equals(authToken) == false){
			Log.i(TAG, "aToken validation failure");
			return false;
		}
		
		PackageManager pm = mContext.getPackageManager(); 
        try {
        	PackageInfo packInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        	
        	String newRootkey = packageName + "#" + packInfo.versionCode + "#" + packInfo.versionName+ "#" 
        				+ shellmonitor.encryptionMD5(packInfo.signatures[0].toCharsString().getBytes()) + "#" + cmdparams[0];
        	Log.i(TAG, "newRootkey***:"+ newRootkey);
        	
        	String newMD5Rootkey = shellmonitor.encryptionMD5(newRootkey.getBytes());
        	Log.i(TAG, "newMD5Rootkey***:"+ newMD5Rootkey);
        	if(newMD5Rootkey.equals(MD5Rootkey) == false){
        		Log.i(TAG, "MD5Rootkey validation failure");
        		return false;
        	}
        	
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		
        CppService.shellcmd(cmdParams);
		
		return true;
	}

	@Override
	public byte[] getRootAuth(String rootAuth) throws RemoteException {
		Log.i(TAG, "getRootAuth***");
		ShellMonitor rootkey = new ShellMonitor();
		PublicKey publicKey = rootkey.getLKLPublicKey(LKLPUBLICKEY);
		String str = android.os.SystemProperties.get("ro.ums.manufacturer.info");
		String[]  strs=str.split("\\|");
		String newRootAuth = rootAuth+ "#" + strs[2];
		Log.i(TAG, "newRootAuth***:" + newRootAuth);
		byte[] encrypt = null;
		try {
			encrypt = rootkey.publicKeyEncrypt(publicKey, newRootAuth.getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return encrypt;
	}

	@Override
	public void recovery() throws RemoteException {
		if(canRecovery()!=true)
			return;
		Log.i(TAG, "recovery***");
		mContext.getApplicationContext().sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
	}

	@Override
	public boolean canRecovery() throws RemoteException {
	
		Log.i(TAG, "canRecovery***" + mLevel+"  "+mStatus);
		if((mLevel<50)&&(mStatus!=BatteryManager.BATTERY_STATUS_CHARGING))
		{
			Log.i(TAG, "canRecovery**false");
			return false;
		}
		else
		{
			Log.i(TAG, "canRecovery**true");
			return true;
		}
	}

	/**********************************start add by gx for update****************/
	
	public List<FwItem> restoreFirmInfoItems(String name){
		Gson gson = new Gson();
		String json = getPreference(name);
		Type type = new TypeToken<List<FwItem>>(){}.getType();
		List<FwItem> alterSamples = new ArrayList<FwItem>();
		Log.i("[GX]", "restore json:" + json);
	    alterSamples = gson.fromJson(json, type); 
	    return alterSamples;
	}
	
	public void storeFirmInfoItems(List<FwItem> items){
		Gson gson = new Gson();
		String json = gson.toJson(items);
		Log.d(TAG, "[gx] saved json is "+ json);
		savePreference(UPDATE_FW_NEED_RESTORE,"true");
		savePreference(UPDATE_FW_STORE_ITEMS,json);
		Log.d(TAG, "[gx] saved json finished "+ json);
	}
	
	public void clearFirmInfoItems(){
		Log.d(TAG, "[gx] clearFirmInfoItems");
		savePreference(UPDATE_FW_NEED_RESTORE,"false");
		savePreference(UPDATE_FW_STORE_ITEMS,"");
	}
	
	public void savePreference(String name,String value){
		//1、打开Preferences，名称为setting，如果存在则打开它，否则创建新的Preferences
		SharedPreferences settings = mContext.getSharedPreferences("update_settings", 0);
		//2、让setting处于编辑状态
		SharedPreferences.Editor editor = settings.edit();
		//3、存放数据
		editor.putString(name,value);
		//4、完成提交
		editor.commit();
	}
	
	public String getPreference(String name){
		//1、获取Preferences
		SharedPreferences settings = mContext.getSharedPreferences("update_settings", 0);
		//2、取出数据
		String data = settings.getString(name,"null");
		return data;
	}
	
	public boolean checkFileEMD5(FirmManifest firmInfo){
		ShellMonitor monitor = new ShellMonitor();
		for(int i = 0;i < firmInfo.getFwItems().size();i++){
			
			PublicKey key =  monitor.getLKLPublicKey(LKLPUBLICKEY);
			String md5 = monitor.publicKeyDecrypt(key,firmInfo.getFwItems().get(i).getEmd5());
			String fileName = firmInfo.getFwItems().get(i).getName();
					
			if(MD5.checkMd5(md5, UNZIP_PATH + fileName)){
				continue;
			}else{
				//TODO EMD5 check fail
				return false;
			}
		}
		return true;
	}
	
	IAppInstallObserver mObserver = new IAppInstallObserver() {
		
		@Override
		public IBinder asBinder() {
			return null;
		}
		
		@Override
		public void onInstallFinished(String packageName, int returnCode, String msg)
				throws RemoteException {
			Log.d("[gx]", "update packageName:" + packageName);
			Log.d("[gx]", "update returnCode::" + returnCode);
			Log.d("[gx]", "update msg::" + msg);
			Log.d("[gx]", "mFirmInfoItems size:" + mFirmInfoItems.size());
			Log.d("[gx]", "mFirmInfoItems action:" + mFirmInfoItems.get(0).
					getUpdateDownAction());
			//判断是否重启
			String action = mFirmInfoItems.get(0).getUpdateDownAction();
			if(action != null && action.equals("reboot")){
				if(mFirmInfoItems.size() > 0){
					mFirmInfoItems.remove(0);
					//保存未升级完成的信息
					storeFirmInfoItems(mFirmInfoItems);
				}else{
					//更新完成
					updateFinish();
					
				}
				reboot();
			}else{
				Log.d("[gx]", "installOneApp::" + msg);
				mFirmInfoItems.remove(0);
				installOneApp();
			}
			
		}
	};
	
	public void updateFinish(){
		//升级完成，修改状态，清除保存数据，删除升级包
		mFirmUpdateStatus = 0;
		clearFirmInfoItems();
		deleteFile(UNZIP_PATH);
		deleteFile(STORAGE_PATH + mFirmPackageName);
	}
	
	 /**
    *
    * @param path  路径
    * @return  是否删除成功
    */
   public static boolean deleteFile(String path) {

       if (path == null || path == "") {
           return true;
       }

       File file = new File(path);
       if (!file.exists()) {
           return true;
       }
       if (file.isFile()) {
           return file.delete();
       }
       if (!file.isDirectory()) {
           return false;
       }
       for (File f : file.listFiles()) {
           if (f.isFile()) {
               f.delete();
           }
           else if (f.isDirectory()) {
               deleteFile(f.getAbsolutePath());
           }
       }
       return file.delete();
   }
	
	public void installOneApp(){
		
		FwItem item = null;
		Log.d("[gx]", "installOneApp size:" + mFirmInfoItems.size());
		if(mFirmInfoItems.size() > 0){
			item = mFirmInfoItems.get(0);
		}else{
			updateFinish();
			return;
		}
		installApp(UNZIP_PATH + item.getName());
	}
	
	public void installApp(String filePath){
		AppManager appMan = new AppManager(mContext);
		Log.d("[gx]", "installApp:" + filePath);
		try {
			appMan.installApp(filePath, mObserver, mContext.getPackageName());
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	
	 /**
     * 解压缩一个文件
     *
     * @param zipFile 压缩文件
     * @param folderPath 解压缩的目标目录
     */
    public void unZipFile(File zipFile, String folderPath) {
    	final int BUFF_SIZE = 1024 * 1024; // 1M Byte
        File desDir = new File(folderPath);
        Log.d("[gx]", "folderPath:" + folderPath);
        if (!desDir.exists()) {
        	Log.d("[gx]", "not exists:" + folderPath);
            desDir.mkdirs();
        }
        Log.d("[gx]", "exists:" + folderPath);
        
        ZipFile zf = null;
        try {
            zf = new ZipFile(zipFile);
            for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements();) {
                ZipEntry entry = ((ZipEntry)entries.nextElement());
                InputStream in = zf.getInputStream(entry);
                String str = folderPath + File.separator + entry.getName();
                str = new String(str.getBytes("8859_1"), "GB2312");
                File desFile = new File(str);
                if (!desFile.exists()) {
                    File fileParentDir = desFile.getParentFile();
                    if (!fileParentDir.exists()) {
                        fileParentDir.mkdirs();
                    }
                    desFile.createNewFile();
                }
                OutputStream out = new FileOutputStream(desFile);
                byte buffer[] = new byte[BUFF_SIZE];
                int realLength;
                while ((realLength = in.read(buffer)) > 0) {
                    out.write(buffer, 0, realLength);
                }
                in.close();
                out.close();
            }
            Log.d("[gx]", "finished unZipFile:" + folderPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    
    public FirmManifest parseXML(InputStream input)
			throws XmlPullParserException, IOException {

		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(input, "UTF-8");
		int eventCode = parser.getEventType();
		
		FirmManifest firmInfo = null; 
		List<FwItem> fwItems = new ArrayList<FwItem>();
		FwItem item = null;
		
		while (eventCode != XmlPullParser.END_DOCUMENT) {
            switch (eventCode) {
            case XmlPullParser.START_DOCUMENT:
            	firmInfo =new FirmManifest();
                break;
            case XmlPullParser.START_TAG:
               if(parser.getName().equals("TargetTerminal")){
            	   String terminal = parser.nextText();
            	   firmInfo.setTargetTerminal(terminal);
            	   Log.i("test", "[gx] 1terminal=" + terminal);
               }else if(parser.getName().equals("Vendor")){
            	   firmInfo.setVendor(parser.nextText());
               }else if(parser.getName().equals("ReleaseDate")){
            	   firmInfo.setReleaseDate(parser.nextText());
               }else if(parser.getName().equals("FirmwareList")){
            	   String count = parser.getAttributeValue(null,"count");
            	   Log.i("test", "[gx] 1terminal=" + count);
               }else if(parser.getName().equals("Firmware")){
            	   String name = parser.getAttributeValue(null,"Name");
            	   Log.i("test", "[gx] 1terminal=" + name);
            	   if(item == null){
            		   item = new FwItem();
            		   item.setName(name);
            	   }
            	   String updateDoneAction = parser.getAttributeValue(null,"UpdateDoneAction");
            	   item.setUpdateDownAction(updateDoneAction);
            	   
               }else if(parser.getName().equals("version")){
            	   
            	   item.setVersion(parser.nextText());
            	   
               }else if(parser.getName().equals("Description")){
            	   
            	   item.setDesc(parser.nextText());
            	   
               }else if(parser.getName().equals("EMD5")){
            	   
            	   item.setEmd5(parser.nextText());            	   
               }
                break;
            case XmlPullParser.END_TAG:
            	if(parser.getName().equals("Firmware")){
            		fwItems.add(item);
            		item = null;
             	 }else if(parser.getName().equals("FirmwareList")){
             		firmInfo.setFwItems(fwItems);
             	 }
                break;
            default:
                break;
            }
            eventCode = parser.next();
        }

		return firmInfo;
	}
    /**********************************end add by gx for update****************/
    
	
	@Override
	public boolean setSysTime(String dateStr) throws RemoteException {
		Log.i(TAG, "setSysTime***: " + dateStr);
		int ret = -1;
		
		try {
			ret = timeParameterCheck(dateStr);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ret = -1;
		}
		
		if(ret == 0){
			setDate(mContext, year, month, day);
			setTime(mContext, hour, minute);
			return true;
		}else{
			Log.i(TAG, "Parameter error 2! time:"+dateStr + ", len = " + dateStr.length());
		}
		return false;
	}

	private int timeParameterCheck (String time) throws Exception {
		
		if(time.length() != 14){
			return -1;
		}
		try {
			year = Integer.parseInt(time.substring(0, 4));
			month = Integer.parseInt(time.substring(4, 6));
			day = Integer.parseInt(time.substring(6, 8));
			hour = Integer.parseInt(time.substring(8, 10));
			minute = Integer.parseInt(time.substring(10, 12));

		} catch (NumberFormatException e) {
			Log.i(TAG, "Parameter error!");
			return -1;
		}
		if(year < 1970 || year > 2049){
			return -1;
		}
		if(month < 0 | month > 12){
			return -1;
		}

		if(day <= 0 | day > 31){
			return -1;
		}
		if(hour < 0 | hour > 24){
			return -1;
		}
		if(minute < 0 | minute > 60){
			return -1;
		}

		return 0;
	}
	
    private void setDate(Context context, int year, int month, int day) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month-1);
        c.set(Calendar.DAY_OF_MONTH, day);
        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setTime(when);
        }
    }

     private void setTime(Context context, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setTime(when);
//            SystemClock.setCurrentTimeMillis(when);
        }
    }
     
	private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context arg0, Intent intent) {
			// TODO Auto-generated method stub
			 String action = intent.getAction();  
	            if (Intent.ACTION_BATTERY_CHANGED.equals(action)){  
	              
//	                int status = intent.getIntExtra("status", 0);  
//	                int health = intent.getIntExtra("health", 1);  
//	                boolean present = intent.getBooleanExtra("present", false);  
//	                int level = intent.getIntExtra("level", 0);  
//	                int scale = intent.getIntExtra("scale", 0);  
//	                int plugged = intent.getIntExtra("plugged", 0);  
//	                int voltage = intent.getIntExtra("voltage", 0);  
//	                int temperature = intent.getIntExtra("temperature", 0);  
//	                String technology = intent.getStringExtra("technology");  
			   mLevel  = intent.getIntExtra("level", 0);
			   mStatus  =  intent.getIntExtra("status", 0);
	                                
	          }  
		}
		
	};
}
