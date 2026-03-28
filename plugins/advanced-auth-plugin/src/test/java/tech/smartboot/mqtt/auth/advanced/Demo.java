package tech.smartboot.mqtt.auth.advanced;

import tech.smartboot.mqtt.auth.advanced.provider.AbstractAuthenticator;
import tech.smartboot.mqtt.auth.advanced.provider.RedisAuthenticator;
import tech.smartboot.redisun.Redisun;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 三刀
 * @version v1.0 3/28/26
 */
public class Demo {
    public static void main(String[] args) {
        Redisun redisun = Redisun.create(options -> {
            options.setAddress("redis://127.0.0.1:6379")
                    .setDatabase(0);
        });

        // 1. 创建 base64 加密的账号（使用用户专属的 base64 算法）
        AbstractAuthenticator authenticator = new RedisAuthenticator(null);
        SecureRandom secureRandom = new SecureRandom();
        byte[] base64SaltBytes = new byte[16];
        secureRandom.nextBytes(base64SaltBytes);
        String base64Salt = bytesToHex(base64SaltBytes);
        Map<String, String> base64Values = new HashMap<>();
        String base64PasswordHash = PluginUtil.encodePassword(base64Salt + "base64_pass", "base64");
        base64Values.put("password_hash", base64PasswordHash);
        base64Values.put("salt", base64Salt);
        base64Values.put("password_encoder", "base64"); // 指定该用户的签名算法
        redisun.hmset("smart-mqtt:auth:base64_user", base64Values);

        // 2. 创建 sha256 加密的账号（使用用户专属的 sha256 算法）
        byte[] sha256SaltBytes = new byte[16];
        secureRandom.nextBytes(sha256SaltBytes);
        String sha256Salt = bytesToHex(sha256SaltBytes);
        Map<String, String> sha256Values = new HashMap<>();
        String sha256PasswordHash = PluginUtil.encodePassword(sha256Salt + "sha256_pass", "sha256");
        sha256Values.put("password_hash", sha256PasswordHash);
        sha256Values.put("salt", sha256Salt);
        sha256Values.put("password_encoder", "sha256"); // 指定该用户的签名算法
        redisun.hmset("smart-mqtt:auth:sha256_user", sha256Values);

        // 3. 创建 plain 明文加密的账号（使用用户专属的 plain 算法，不推荐用于生产环境）
        byte[] plainSaltBytes = new byte[16];
        secureRandom.nextBytes(plainSaltBytes);
        String plainSalt = bytesToHex(plainSaltBytes);
        Map<String, String> plainValues = new HashMap<>();
        String plainPasswordHash = PluginUtil.encodePassword(plainSalt + "plain_pass", "plain");
        plainValues.put("password_hash", plainPasswordHash);
        plainValues.put("salt", plainSalt);
        plainValues.put("password_encoder", "plain"); // 指定该用户的签名算法
        redisun.hmset("smart-mqtt:auth:plain_user", plainValues);

        // 4. 创建不使用盐值的账号（仅使用密码加密）
        Map<String, String> md5Values = new HashMap<>();
        String md5PasswordHash = PluginUtil.encodePassword("md5_pass", "md5"); // 不加盐
        md5Values.put("password_hash", md5PasswordHash);
        md5Values.put("salt", ""); // 空盐值
        md5Values.put("password_encoder", "md5"); // 指定该用户的签名算法
        redisun.hmset("smart-mqtt:auth:md5_user", md5Values);

        System.out.println("认证账号创建成功！");
        System.out.println("===========================================");
        System.out.println("Base64 加密账号：base64_user / base64_pass (盐值：" + base64Salt + ", 算法：base64)");
        System.out.println("SHA256 加密账号：sha256_user / sha256_pass (盐值：" + sha256Salt + ", 算法：sha256)");
        System.out.println("Plain 明文账号：plain_user / plain_pass (盐值：" + plainSalt + ", 算法：plain)");
        System.out.println("MD5 无盐账号：md5_user / md5_pass (无盐值，算法：md5)");
        System.out.println("===========================================");
        System.out.println("\n注意：");
        System.out.println("- 每个用户可以使用不同的签名算法，增强灵活性");
        System.out.println("- base64 和 sha256 提供了基本的安全性");
        System.out.println("- plain 为明文存储，仅用于测试，不建议在生产环境使用");
        System.out.println("- 所有账号均使用独立随机盐值（除 md5_user 外），增强安全性");
        System.out.println("- password_encoder 字段允许每个用户使用不同的加密算法");
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
