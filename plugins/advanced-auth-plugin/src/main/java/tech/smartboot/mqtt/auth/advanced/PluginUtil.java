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
     * @param encoderType 编码器类型（plain/sha256/md5）
     * @return 编码后的密码
     */
    public static String encodePassword(String rawPassword, String encoderType) {
        if ("sha256".equalsIgnoreCase(encoderType)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 algorithm not available", e);
            }
        }

        if ("md5".equalsIgnoreCase(encoderType)) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 algorithm not available", e);
            }
        }

        // 默认返回明文
        return rawPassword;
    }
}
