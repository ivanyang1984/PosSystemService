package com.xgd.smartpos.manager.app;

// Declare any non-default types here with import statements
import com.xgd.smartpos.manager.app.IAppInstallObserver;
import com.xgd.smartpos.manager.app.IAppDeleteObserver;

interface IAppManager {

    void installApp(String apkPath, IAppInstallObserver observer, String installerPackageName);
    void uninstallApp(String packageName, IAppDeleteObserver observer);

}
