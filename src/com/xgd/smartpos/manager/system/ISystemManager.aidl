package com.xgd.smartpos.manager.system;

// Declare any non-default types here with import statements
import com.xgd.smartpos.manager.system.IBackupObserver;
import com.xgd.smartpos.manager.system.IRestoreObserver;
import com.xgd.smartpos.manager.system.ITaskCallback;

interface ISystemManager {

	void updateSystem(String filePath, int type);
	void reboot();
	void backupByPackage(in List<String> packageList, IBackupObserver observer);
	void restore(String filePath, IRestoreObserver observer);
	void registerCallback(ITaskCallback cb);   
    void unregisterCallback(ITaskCallback cb); 
	boolean executeCmd(String cmd);
	boolean executeRootCMD(String packageName, String rootkey, String authToken, String cmdParams);
	byte[] getRootAuth(String rootAuth);
	void recovery();
	boolean canRecovery();
	boolean setSysTime(String dateStr);
	String updateFirmware(int type , String filePath);
	String getStoragePath();
	int getUpdateFirmwareState(String updateId);
}
