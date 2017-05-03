#
# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src) \
   src/com/xgd/smartpos/manager/device/IDeviceManager.aidl \
   src/com/xgd/smartpos/manager/app/IAppDeleteObserver.aidl \
   src/com/xgd/smartpos/manager/app/IAppInstallObserver.aidl \
   src/com/xgd/smartpos/manager/app/IAppManager.aidl \
   src/com/xgd/smartpos/manager/ICloudService.aidl \
   src/com/xgd/smartpos/manager/system/ISystemManager.aidl \
   src/com/xgd/smartpos/manager/system/ITaskCallback.aidl \
   src/com/xgd/smartpos/manager/system/IBackupObserver.aidl \
   src/com/xgd/smartpos/manager/system/IRestoreObserver.aidl \
   src/com/xgd/smartpos/nativemanager/IDataService.aidl


LOCAL_STATIC_JAVA_LIBRARIES := gson 
LOCAL_CERTIFICATE := platform
LOCAL_PACKAGE_NAME := PosSystemService
LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)

include $(CLEAR_VARS)


#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
#    gson:libs/gson-2.2.4.jar

#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := \
#    libccbcloudsdk:libs/ccb-cloudsdk-1.0.0-20161027.jar


include $(BUILD_MULTI_PREBUILT)

