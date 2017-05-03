
package com.xgd.smartpos.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.Log;

public class MD5 {
    static String TAG = "MD5";

	private static String createMd5(String str) {
		MessageDigest mMDigest;
		FileInputStream Input;
		File file = new File(str);
		byte buffer[] = new byte[1024];
		int len;
		if (!file.exists())
			return null;
		try {
			mMDigest = MessageDigest.getInstance("MD5");
			Input = new FileInputStream(file);
			while ((len = Input.read(buffer, 0, 1024)) != -1) {
				mMDigest.update(buffer, 0, len);
			}
			Input.close();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		BigInteger mBInteger = new BigInteger(1, mMDigest.digest());
		Log.v(TAG, "create_MD5=" + mBInteger.toString(16));
		return mBInteger.toString(16);

	}

    public static boolean checkMd5(String Md5, String file) {
    	
    	if(Md5 == null || file == null){
			return false;
		}
    	
		String str = createMd5(file);
		if(str == null){
			return false;
		}
		int length = str.length();
		
		Log.d(TAG, "str = " + str + ";  length = " + length);
		Log.d(TAG, "Md5 = " + Md5 + ";  length = " + Md5.length());

		if (length < 32) {
			Log.d(TAG,
					"the length of the created md5sum is wrong, add 0s in the front");
			int temp = 32 - length;
			int i = 0;
			String sup = "0";
			for (i = 1; i < temp; i++) {
				sup = sup + "0";
			}
			str = sup + str;
			Log.d(TAG, "md5sum = " + str + ";  added " + temp
					+ " 0s in the front");
		}
		if (Md5.compareToIgnoreCase(str) == 0)
			return true;
		else
			return false;
    }
}
