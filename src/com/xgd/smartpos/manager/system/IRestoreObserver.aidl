package com.xgd.smartpos.manager.system;

// Declare any non-default types here with import statements

interface IRestoreObserver {

	void onRestoreFinished(int returnCode, String msg);
	
}