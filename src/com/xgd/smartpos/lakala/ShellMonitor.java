package com.xgd.smartpos.lakala;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

import com.xgd.smartpos.utils.TagString;

import android.util.Base64;
import android.util.Log;

public class ShellMonitor {
	private final String TAG = TagString.NAME;
	
	public PublicKey getLKLPublicKey(String keyfile){
    	RandomAccessFile randomFile = null;
//    	String keyfile = "/tmp/lakala.key";
    	try {
			randomFile = new RandomAccessFile(keyfile, "r");
			File key = new File(keyfile);
			byte[] keydata = new byte[(int) key.length()];
			randomFile.read(keydata);
			byte[] keyDecode = Base64.decode(keydata, Base64.DEFAULT);
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyDecode); 
			KeyFactory factory = KeyFactory.getInstance("RSA"); 
			PublicKey publicKey = factory.generatePublic(spec);
//			Log.i(TAG, "publicKey="+publicKey.toString());
			return publicKey;
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			try {
				randomFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    	return null;
    }
	
	/**
	 * 利用公钥加密，再用base64编码
	 * @param publicKey :公钥
	 * @param plainTextData ：待加密数据
	 * @return ：加密后数据
	 * @author lizejin---20170227
	 */
	public byte[] publicKeyEncrypt(PublicKey publicKey, byte[] plainTextData)   {  

        Cipher cipher = null;  
        try {  
            cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");  
            // cipher= Cipher.getInstance("RSA", new BouncyCastleProvider());  
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);  
            byte[] output = cipher.doFinal(plainTextData); 
            byte[] keyencode = Base64.encode(output, Base64.DEFAULT);
            Log.i(TAG, "keyencode="+new String(keyencode));
            return keyencode;  
        } catch (Exception e) {  
        	e.printStackTrace(); 
        } 
        return null;
    } 
	
	public String publicKeyDecrypt(PublicKey publicKey, String signval){
		Cipher cipher;
		String decrypted = null;
       try {
           cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
           cipher.init(Cipher.DECRYPT_MODE, publicKey);

           byte[] keyencode = Base64.decode(signval, Base64.DEFAULT);           
           byte[] decryptedBytes = cipher.doFinal(keyencode);  
           decrypted = new String(decryptedBytes);
           Log.i(TAG, "decrypted: "+decrypted);
       }  catch (Exception e) {
           e.printStackTrace();
           Log.i(TAG, "Exception: "+e);
       }	
       return decrypted;
	}
	
	
	/** 
	 * MD5加密 
	 * @param byteStr 需要加密的内容 
	 * @return 返回 byteStr的md5值 
	 */
	public String encryptionMD5(byte[] byteStr) { 
		MessageDigest messageDigest = null;
		StringBuffer md5StrBuff = new StringBuffer();
		try {
			messageDigest = MessageDigest.getInstance("MD5");
			messageDigest.reset();
			messageDigest.update(byteStr);
			byte[] byteArray = messageDigest.digest(); 
			for (int i = 0; i < byteArray.length; i++) {
				if (Integer.toHexString(0xFF & byteArray[i]).length() == 1) { 
					md5StrBuff.append("0").append(Integer.toHexString(0xFF & byteArray[i])); 
				}else {
					md5StrBuff.append(Integer.toHexString(0xFF & byteArray[i])); 
				}
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return  md5StrBuff.toString();
	}
	private void printHexString(String info, byte[] b)
    {
        StringBuffer tmpstr = new StringBuffer();
        tmpstr.append(info);

        for (int i = 0; i < b.length; i++)
        {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1)
            {
                hex = '0' + hex;
            }
            tmpstr.append(hex.toUpperCase() + " ");
        }
        Log.i(TAG, tmpstr.toString());
    }
	private String bytearrayToCharString(byte[] b){
   		StringBuffer strbuf = new StringBuffer();

   		for (int i = 0; i < b.length; i++)
   		{
   			if (b[i] == '\0')
   			{
   				break;
   			}
   			strbuf.append((char) b[i]);
   		}

   		return strbuf.toString();
   	}
}
