package com.ranqiyun.service.web.util;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.security.SecureRandom;


public class DesUtil {
    private final static String DES = "DES";
    private final static String ENCODE = "UTF-8";
    private final static String defaultKey = "NhY8gt7y";//8字节key长度

    public static String encrypt(String express) throws Exception {
        return encrypt(express, defaultKey);
    }

    public static String decrypt(String ciphertext) throws Exception {
        return decrypt(ciphertext, defaultKey);
    }

    public static String encrypt(String express, String key) throws Exception {
        if (express == null) return null;
        return Base64.urlSafeEncode(encrypt(express.getBytes(ENCODE), key.getBytes(ENCODE)));
    }

    public static String decrypt(String ciphertext, String key) throws Exception {
        if (ciphertext == null) return null;
        return new String(decrypt(Base64.urlSafeDecode(ciphertext), key.getBytes(ENCODE)), ENCODE);
    }

    public static byte[] encrypt(byte[] data, byte[] key) throws Exception {
        return do_cipher(Cipher.ENCRYPT_MODE, data, key);
    }

    public static byte[] decrypt(byte[] data, byte[] key) throws Exception {
        return do_cipher(Cipher.DECRYPT_MODE, data, key);
    }

    private static byte[] do_cipher(int mode, byte[] data, byte[] key) throws Exception {
        // 生成一个可信任的随机数源
        SecureRandom sr = new SecureRandom();
        // 从原始密钥数据创建DESKeySpec对象
        DESKeySpec dks = new DESKeySpec(key);
        // 创建一个密钥工厂，然后用它把DESKeySpec转换成SecretKey对象
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DES);
        SecretKey securekey = keyFactory.generateSecret(dks);
        // Cipher对象实际完成解密操作
        Cipher cipher = Cipher.getInstance(DES);
        // 用密钥初始化Cipher对象
        cipher.init(mode, securekey, sr);
        return cipher.doFinal(data);
    }


    public static void main(String[] args) throws Exception {
        String express = "DES加密解密算法";
        String ciphertext = encrypt(express, "12345678");
        System.out.println(ciphertext);
        String rexpress = decrypt(ciphertext, "12345678");
        System.out.println(express);
    }
}
