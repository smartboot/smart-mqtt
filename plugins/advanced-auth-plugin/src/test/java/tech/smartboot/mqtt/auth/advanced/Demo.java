package tech.smartboot.mqtt.auth.advanced;

import tech.smartboot.redisun.Redisun;

import java.security.SecureRandom;
import java.util.Base64;
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

        SecureRandom secureRandom = new SecureRandom();
        // 2. 创建 sha256 加密的账号（使用用户专属的 sha256 算法）
        byte[] sha256SaltBytes = new byte[16];
        secureRandom.nextBytes(sha256SaltBytes);
        String sha256Salt = Base64.getEncoder().encodeToString(sha256SaltBytes);
        Map<String, String> sha256Values = new HashMap<>();
        String sha256PasswordHash = PluginUtil.encodePassword(sha256Salt + "sha256_pass", "sha256");
        sha256Values.put("pwd_hash", sha256PasswordHash);
        sha256Values.put("salt", sha256Salt);
        sha256Values.put("encoder", "sha256"); // 指定该用户的签名算法
        redisun.hmset("smart-mqtt:auth:sha256_user", sha256Values);

        // 3. 创建 plain 明文加密的账号（使用用户专属的 plain 算法，不推荐用于生产环境）
        byte[] plainSaltBytes = new byte[16];
        secureRandom.nextBytes(plainSaltBytes);
        String plainSalt = Base64.getEncoder().encodeToString(plainSaltBytes);
        Map<String, String> plainValues = new HashMap<>();
        String plainPasswordHash = PluginUtil.encodePassword(plainSalt + "plain_pass", "plain");
        plainValues.put("pwd_hash", plainPasswordHash);
        plainValues.put("salt", plainSalt);
        plainValues.put("encoder", "plain"); // 指定该用户的签名算法
        redisun.hmset("smart-mqtt:auth:plain_user", plainValues);

        // 4. 创建不使用盐值的账号（仅使用密码加密）
        Map<String, String> md5Values = new HashMap<>();
        String md5PasswordHash = PluginUtil.encodePassword("md5_pass", "md5"); // 不加盐
        md5Values.put("pwd_hash", md5PasswordHash);
        md5Values.put("salt", ""); // 空盐值
        md5Values.put("encoder", "md5"); // 指定该用户的签名算法
        redisun.hmset("smart-mqtt:auth:md5_user", md5Values);

        System.out.println("认证账号创建成功！");
        System.out.println("===========================================");
        System.out.println("SHA256 加密账号：sha256_user / sha256_pass (盐值：" + sha256Salt + ", 算法：sha256)");
        System.out.println("Plain 明文账号：plain_user / plain_pass (盐值：" + plainSalt + ", 算法：plain)");
        System.out.println("MD5 无盐账号：md5_user / md5_pass (无盐值，算法：md5)");
        System.out.println("===========================================");
        System.out.println("\n注意：");
        System.out.println("- 每个用户可以使用不同的签名算法，增强灵活性");
        System.out.println("- base64 和 sha256 提供了基本的安全性");
        System.out.println("- plain 为明文存储，仅用于测试，不建议在生产环境使用");
        System.out.println("- 所有账号均使用独立随机盐值（除 md5_user 外），增强安全性");
        System.out.println("- encoder 字段允许每个用户使用不同的加密算法");
    }

}
