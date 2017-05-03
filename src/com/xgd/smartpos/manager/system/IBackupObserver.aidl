package com.xgd.smartpos.manager.system;

// Declare any non-default types here with import statements

interface IBackupObserver {

	void onBackupFinished(int returnCode, String msg, String filePath);
	
}