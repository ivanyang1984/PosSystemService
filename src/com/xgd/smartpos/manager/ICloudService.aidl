
package com.xgd.smartpos.manager;



interface ICloudService {

    IBinder getManager(int type);
    String getServiceSdkVersion();
}