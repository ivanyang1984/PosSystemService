package com.xgd.smartpos.manager.app;

// Declare any non-default types here with import statements

interface IAppInstallObserver {

    void onInstallFinished(String packageName, int returnCode, String msg);

}