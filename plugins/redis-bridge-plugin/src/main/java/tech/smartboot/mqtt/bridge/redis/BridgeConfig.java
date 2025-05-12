package tech.smartboot.mqtt.bridge.redis;

import tech.smartboot.mqtt.common.enums.PayloadEncodeEnum;

import java.util.List;

public class BridgeConfig {

    private List<RedisConfig> redis;

    public List<RedisConfig> getRedis() {
        return redis;
    }

    public void setRedis(List<RedisConfig> redis) {
        this.redis = redis;
    }

    public static class RedisConfig {
        private String address;

        private int database;

        private String password;

        private String encode = PayloadEncodeEnum.STRING.getCode();

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getEncode() {
            return encode;
        }

        public void setEncode(String encode) {
            this.encode = encode;
        }
    }
}
