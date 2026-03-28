package tech.smartboot.mqtt.auth.advanced;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @author 三刀
 * @version v1.0 3/28/26
 */
public class PluginUtil {
    /**
     * 对密码进行编码
     *
     * @param rawPassword 明文密码
     * @param encoderType 编码器类型（plain/sha256/base64/md5）
     * @return 编码后的密码
     */
    public static String encodePassword(String rawPassword, String encoderType) {
        if (rawPassword == null) {
            return null;
        }

        if (encoderType == null || encoderType.isEmpty() || "plain".equalsIgnoreCase(encoderType)) {
            return rawPassword;
        }

        if ("sha256".equalsIgnoreCase(encoderType)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
                return bytesToHex(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 algorithm not available", e);
            }
        }

        if ("base64".equalsIgnoreCase(encoderType)) {
            return Base64.getEncoder().encodeToString(rawPassword.getBytes(StandardCharsets.UTF_8));
        }

        if ("md5".equalsIgnoreCase(encoderType)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
                return bytesToHex(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 algorithm not available", e);
            }
        }

        // 默认返回明文
        return rawPassword;
    }

    /**
     * 字节数组转十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
