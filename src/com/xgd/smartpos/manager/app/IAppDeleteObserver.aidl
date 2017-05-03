package com.xgd.smartpos.manager.app;

// Declare any non-default types here with import statements

interface IAppDeleteObserver {

    void onDeleteFinished(String packageName, int returnCode, String msg);

}