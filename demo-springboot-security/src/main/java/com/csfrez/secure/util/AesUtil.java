package com.csfrez.secure.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.RandomUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @date 2025/11/4 18:07
 * @description
 */
public class AesUtil {

    /**
     * 对称加密具体算法实现
     */
    private static final String AES_ALGORITHM = "AES/CFB/NoPadding";

    private static final String AES = "AES";

    /**
     * 加密
     *
     * @param content 加密内容
     * @param key     密钥
     * @return 加密结果
     * @throws Exception 加密异常
     */
    public static String encrypt(String content, String key) throws Exception {
        try {
            byte[] contentBytes = content.getBytes();
            byte[] keyBytes = key.getBytes();
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES);
            IvParameterSpec ivSpec = new IvParameterSpec(keyBytes);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encryptResult = cipher.doFinal(contentBytes);
            return Base64.encode(encryptResult);
            //return Base64.encodeBase64String(encryptResult);
        } catch (Exception e) {
            throw new Exception("AesUtil Encrypt Exception，error=" + e.getMessage());
        }
    }


    /**
     * 解密
     *
     * @param content 解密内容
     * @param key     密钥
     * @return 解密结果
     * @throws Exception 解密异常
     */
    public static String decrypt(String content, String key) throws Exception {
        try {
            byte[] keyBytes = key.getBytes();
            IvParameterSpec ivSpec = new IvParameterSpec(keyBytes);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, AES);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            // base64解密
            byte[] contentBytes = Base64.decode(content);
            byte[] original = cipher.doFinal(contentBytes);
            String originalString = new String(original);
            return originalString;
        } catch (Exception e) {
            throw new Exception("AesUtil Decrypt Exception，error=" + e.getMessage());
        }
    }

    /**
     * 生成16位AES随机密钥
     *
     * @return
     */
    public static String getAESRandomKey() {
        /*
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            Random rand = new Random();
            int index = rand.nextInt(characters.length());
            key.append(characters.charAt(index));
        }
        return key.toString();
        */
        // hutool 写法参考
        String baseString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        return RandomUtil.randomString(baseString,16);
    }

    public static void main(String[] args) throws Exception {
        String content = "123456789";
        String key = getAESRandomKey();
        System.out.println("key：" + key);
        String encrypt = AesUtil.encrypt(content, key);
        System.out.println("AES加密后：" + encrypt);
        String decrypt = AesUtil.decrypt(encrypt, key);
        System.out.println("AES解密后：" + decrypt);
    }


}
