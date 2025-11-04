package com.csfrez.secure.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.lang.Pair;
import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

/**
 * @author csfrez
 * @date 2025/11/4 18:07
 * @description
 */
@Slf4j
public class RsaUtil {

    private final static String ALGORITHM = "RSA";

    /**
     * 加密
     *
     * @param content   加密内容
     * @param publicKey 公钥
     * @return
     * @throws Exception
     */
    public static String encrypt(String content, String publicKey) throws Exception {
        PublicKey key = getPublicKey(publicKey);
        return Base64.encode(encrypt(content.getBytes(), key));
    }

    /**
     * 加密
     *
     * @param content 加密内容
     * @param key     密钥
     * @return
     * @throws Exception
     */
    public static byte[] encrypt(byte[] content, Key key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
//        return cipher.doFinal(content);
        // 获取密钥的最大加密块大小
        int maxBlockSize = getMaxBlockSize(key);
        int contentLength = content.length;
        if (contentLength <= maxBlockSize) {
            // 如果内容长度小于等于单个块大小，直接加密
            return cipher.doFinal(content);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int offset = 0;
        while (contentLength - offset > 0) {
            int blockSize = Math.min(maxBlockSize, contentLength - offset);
            byte[] block = Arrays.copyOfRange(content, offset, offset + blockSize);
            byte[] encryptedBlock = cipher.doFinal(block);
            output.write(encryptedBlock);
            offset += blockSize;
        }
        return output.toByteArray();
    }

    private static int getMaxBlockSize(Key key) throws Exception {
        if (key instanceof RSAPublicKey) {
            RSAPublicKey rsaKey = (RSAPublicKey) key;
            int keyLength = rsaKey.getModulus().bitLength();
            // PKCS#1 padding requires 11 bytes
            return keyLength / 8 - 11;
        } else if (key instanceof RSAPrivateKey) {
            RSAPrivateKey rsaKey = (RSAPrivateKey) key;
            int keyLength = rsaKey.getModulus().bitLength();
            // For decryption
            return keyLength / 8;
        }
        // Default value for 2048-bit keys
        return 245;
    }

    /**
     * 解密
     *
     * @param content    解密内容
     * @param privateKey 私钥
     * @return
     * @throws Exception
     */
    public static String decrypt(String content, String privateKey) throws Exception {
        PrivateKey key = getPrivateKey(privateKey);
        return new String(decrypt(Base64.decode(content), key));
    }

    /**
     * 解密
     *
     * @param content 解密内容
     * @param key     密钥
     * @return
     * @throws Exception
     */
    public static byte[] decrypt(byte[] content, Key key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
//        return cipher.doFinal(content);
        // 获取密钥的最大解密块大小
        int maxBlockSize = getKeyLength(key) / 8;

        if (content.length <= maxBlockSize) {
            // 如果内容长度小于等于单个块大小，直接解密
            return cipher.doFinal(content);
        }

        // 分段解密
        int contentLength = content.length;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int offset = 0;
        while (contentLength - offset > 0) {
            int blockSize = Math.min(maxBlockSize, contentLength - offset);
            byte[] block = Arrays.copyOfRange(content, offset, offset + blockSize);
            byte[] decryptedBlock = cipher.doFinal(block);
            output.write(decryptedBlock);
            offset += blockSize;
        }
        return output.toByteArray();
    }

    private static int getKeyLength(Key key) throws Exception {
        if (key instanceof RSAPublicKey) {
            RSAPublicKey rsaKey = (RSAPublicKey) key;
            return rsaKey.getModulus().bitLength();
        } else if (key instanceof RSAPrivateKey) {
            RSAPrivateKey rsaKey = (RSAPrivateKey) key;
            return rsaKey.getModulus().bitLength();
        }
        // 默认2048位密钥
        return 2048;
    }


    /**
     * 获取公钥
     *
     * @param base64String
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PublicKey getPublicKey(String base64String)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] bytes = Base64.decode(base64String);
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(bytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
        return publicKey;
    }

    /**
     * 获取私钥
     *
     * @param base64String
     * @return
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    public static PrivateKey getPrivateKey(String base64String)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] bytes = Base64.decode(base64String);
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(bytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
        return privateKey;
    }

    /**
     * 生产公私钥
     * @return
     * @throws Exception
     */
    public static Pair<String, String> genKey() throws Exception{
        // 创建RSA密钥对生成器
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        // 设置密钥长度为2048位
        keyPairGenerator.initialize(2048);

        // 生成RSA密钥对
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // 获取公钥和私钥
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // 转换公钥为PKCS#8格式
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
        String publicKeyPKCS8 = java.util.Base64.getEncoder().encodeToString(x509EncodedKeySpec.getEncoded());

        // 转换私钥为PKCS#8格式
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
        String privateKeyPKCS8 = java.util.Base64.getEncoder().encodeToString(pkcs8EncodedKeySpec.getEncoded());

        log.info("公钥（PKCS#8格式）：" + publicKeyPKCS8);
        log.info("私钥（PKCS#8格式）：" + privateKeyPKCS8);
        return Pair.of(publicKeyPKCS8, privateKeyPKCS8);
    }

    public static void main(String[] args) throws Exception {
        // private key
        String privateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCTWtuGrXxa7VL98jnyU+YWt0lu0UmbTkYFqa2G7fhAiT0Qpsx8k9GZ+dbKCvWI0zsLJ3NKYKEcTEBgDKrgwlsIV1HhfJLJcAebE/+C8h++6rOaIdq3MhRK4Z60oSpz7iY21vfdU1SqXpWcDlWu2ncjc96G1QGbuPaOk7YkjODT3VeMazwQYettR36TqgQUNwXRJHyR5E21vSYwkz0aZ10nX99cTPcE7b6hNyvAxoHMdfS/ltF0DlFzW5255gGMDXbTjyHZu0XKQj8mrEeNuus8CLHSv/rYcxMqg/Nw7ihThHsxkni7s1LGVsneOkE9DIuMPdkE/j3SqkuNUFgoVDRLAgMBAAECggEAUH5ZXQB35dOuejpPnShnkBEfdAGvi77+USmXORXVfqt5SBdVrNeyr8HiqwvBhLSelEFAoWiXmbWtlMpWreB9idjpSZubM56XCQerlARfgxMCeTf1Gg2pOB53a4R+hEWbammTjLtwjICOiWjrNVDxs9QfwteowUAQr2HRJbYvOoviJnVA3qN/9QfKhlUs2QrwWqzQhx7WsrNDo6EjlVjin6DmxeavVbj+B0DwvzUhFUBBr9dGaN2EKqhSm6LlN/33xrq/wkwZox2fv+jIlhTiCAi6z45girpPlRO/dGbavk3rHx1OIpTaK8nodUVxT9CF5z1Wx33qDECiKGA7ZdSeAQKBgQDOjA3vpTaj98611rYO3gLhb6YnFH2WVw+q3+ILQTTKM8c93t9xvf8DobbpK+UwDkgCs7jXDXPuQlFHP4fTlm9sJOW6kvC16uv1v9rm302wiJlIwSLjiHkWd132rZjtgTVWdo6PDdg5Dju/pSCpUf/o6E23Kg6m1alQl6yeR6MP6wKBgQC2oriplJzYMCObRa3bXfa/1BlItGGj9zvhjS7E4WdC+RAXAEcY9Aml4xOf4ne2shlqdzy5laf3eFhUgTF+tvQGalbAI8DPCMKO+40vCLeKPYMvAIAWKQ44xW5DpBwzZ6vO4m16laLVEQhq2d2mo9BbE5USxnrDQDPZM0qSMKq1IQKBgC2MDPavxCDcYKUQuA0HQDNeX5F2n0sr37SaskoxEucUC/fE0hlPAJFYc/FvUJcMTyzpXPVvbgU4cpv3kohCQEfkyxQdmx9Css36hNcsnwEy3jqA/KSYT7t4jyv6lCKrv4tVkZJSR0Att1dHnrA3+Orvb1PY3mpD65ZuHA67VWYJAoGACT5hJEweSB7fnns/Wmv+EiV1BiYDDx5zTG4t4M8bv9sFQnTEJHItjtTYaG446ulGugnA7A+0CaVwqw5C6Avwe+aj72Of3Zqu5je7JJdHWtrUKevXj8eSLbNoakoDF2L24ii5q8upCymqiGXwfv+piwKJuAKNshYsyaB79Lw716ECgYA3UvxSfS50OFUaVMoJlN+LqtOO+t4XyXrbPSpEGC8fU3Rw4nPgmRC5jpro8P00tlVORRybGF1rpO0VGmoEwrmPlLz8XAakIS9XPGGd2ya0Rax9AjiARUMlgve8bN7/zSs35H252S68sN64ZZa2fOPvctO8I09PaNW7AcYvRQizCw==";
        // public key
        String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAk1rbhq18Wu1S/fI58lPmFrdJbtFJm05GBamthu34QIk9EKbMfJPRmfnWygr1iNM7CydzSmChHExAYAyq4MJbCFdR4XySyXAHmxP/gvIfvuqzmiHatzIUSuGetKEqc+4mNtb33VNUql6VnA5Vrtp3I3PehtUBm7j2jpO2JIzg091XjGs8EGHrbUd+k6oEFDcF0SR8keRNtb0mMJM9GmddJ1/fXEz3BO2+oTcrwMaBzHX0v5bRdA5Rc1udueYBjA12048h2btFykI/JqxHjbrrPAix0r/62HMTKoPzcO4oU4R7MZJ4u7NSxlbJ3jpBPQyLjD3ZBP490qpLjVBYKFQ0SwIDAQAB";
//        String content = "123456789";

        JSONObject object = new JSONObject();
        //获取token
        object.set("token", "eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6ImM4NjJmY2I2LTM2YzMtNGMzNi1iNmNjLWFlZDY0YWY1MWRiMyJ9.kZCmjbZptSMd0yRJRh0MbI98jLhuhUDKJBqqsmitv6CCLwvsIX9ejOq7l8gg8fk3Wp3_NCSgJrcb-O7E54z9ng");
        object.set("getUrl", "/open/api/user/many/mail");
        //时间戳 毫秒
        object.set("timeStamp", System.currentTimeMillis());
        String content = object.toString();
        System.out.println("RSA加密前：" + content);

        String encryptContent = RsaUtil.encrypt(content, publicKey);
        System.out.println("RSA加密后：" + encryptContent);
        String decryptContent = RsaUtil.decrypt(encryptContent, privateKey);
        System.out.println("RSA解密后：" + decryptContent);
    }
}
