package com.xgd.smartpos.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.xgd.smartpos.nativemanager.IDataService;
import com.xgd.smartpos.systemservice.SystemManager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.StatFs;
import android.util.Log;


public class OtaUpgradeUtils {
	private static final String DATA_PARTITION_CACHE = "/cache/";
	private static final String DATA_PARTITION_DATA  = "/data/data/com.xgd.update/";
	private static String CHCHE_PARTITION = DATA_PARTITION_DATA;
	private static final String DEFAULT_PACKAGE_NAME = "update.zip";
	private Context mContext;
	private final static String TAG = "lizejin";
	private static int mDownloadProgress = 0;
	
	public OtaUpgradeUtils(Context context) {
        mContext = context;
    }
	
    public static int getDownloadProgress(){
        return mDownloadProgress;
    }

	public boolean upgradeFromOta(String otafile, IDataService CppService) {
		File packageFile = new File(otafile);
		if(packageFile.exists() == false){
			return false;
		}

		String filePath =  DATA_PARTITION_CACHE + DEFAULT_PACKAGE_NAME;
        /*
		try {
			CppService.shellcmd("rm -f " + filePath);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
        */
		
		//Cache left space bigger 300M use Cache
        if(getPartitionAvailSizeM(DATA_PARTITION_CACHE) > 300){
        	
        }else{
        	Log.i(TAG, "cache not enough space");
        	return false;
        }
        
        
         
        /*
        Log.i(TAG, "cp -f " + otafile + " " + filePath);
        try {
			CppService.shellcmd("cp " + otafile + " " + filePath);
			CppService.shellcmd("chmod 777 " + filePath);
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}
        installPackage(mContext, new File(filePath));
        */

        boolean b = copyFile(packageFile, new File(filePath));
        Log.i(TAG, "copyFile ok");
        if (b) {
            try {
                Runtime.getRuntime().exec("chmod 777 " + filePath);  
            }catch(IOException e){
                e.printStackTrace();
            }
            installPackage(mContext, new File(filePath));
            return true;
        }

        return false;

	}
	
	private static int getPartitionAvailSizeM(String partition){
    	File file = new File(partition);
        StatFs sf = new StatFs(file.getPath());  
        
		long blockSize = sf.getBlockSize();
//        long blockCount = sf.getBlockCount();
        long availCount = sf.getAvailableBlocks();

        return (int) (availCount*blockSize/1024/1024);
        
    }
	
	private static boolean copyFile(File src, File dst) {
        long inSize = src.length();
        long outSize = 0;

        mDownloadProgress = 0;

        try {
            if (!dst.exists()) {
                dst.createNewFile();
            }
            FileInputStream in = new FileInputStream(src);
            FileOutputStream out = new FileOutputStream(dst);
            int length = -1;
            byte[] buf = new byte[1024];
            while ((length = in.read(buf)) != -1) {
                out.write(buf, 0, length);
                outSize += length;
                int temp = (int) (((float) outSize) / inSize * 100);
                if (temp != mDownloadProgress) {
                    mDownloadProgress = temp;
                    SystemManager.callback(mDownloadProgress); 
                    Log.d("[gx]","copyfile:" + mDownloadProgress);
                }
            }
            out.flush();
            in.close();
            out.close();
            //add by gx when update finish delete update file
            src.delete();
            Runtime.getRuntime().exec("chmod 777 " + dst.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
	
	private static void installPackage(Context context, File packageFile) {
        try { 
        	Log.i(TAG, "RecoverySystem installPackage");
            RecoverySystem.installPackage(context, packageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
}
