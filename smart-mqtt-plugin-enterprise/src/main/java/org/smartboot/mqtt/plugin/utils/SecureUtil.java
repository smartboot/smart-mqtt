package org.smartboot.mqtt.plugin.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SecureUtil {

    public static String shaEncrypt(String input) {
        return encrypt(input, "SHA-256");
    }

    public static String md5(String input) {
        return encrypt(input, "MD5");
    }

    private static String encrypt(String input, String algorithm) {
        try {
            // 获取MessageDigest实例
            MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
            // 对输入字符串进行加密
            byte[] inputBytes = input.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = messageDigest.digest(inputBytes);
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("无法找到指定的加密算法： " + algorithm, e);
        }
    }

    public static void main(String[] args) {
        String input = "需要加密的字符串需要加密的字符串";
        String algorithm = "SHA-256"; // 可以选择其他SHA算法，如"SHA-1", "SHA-512"等
        String encrypted = encrypt(input, algorithm);
        System.out.println("加密后的字符串： " + encrypted);
    }

}
