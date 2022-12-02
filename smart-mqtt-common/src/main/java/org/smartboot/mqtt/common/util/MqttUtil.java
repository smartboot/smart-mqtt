package org.smartboot.mqtt.common.util;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import com.alibaba.fastjson2.JSONReader;
import org.smartboot.mqtt.common.MqttMessageBuilders;
import org.smartboot.mqtt.common.enums.MqttQoS;
import org.smartboot.mqtt.common.message.MqttPublishMessage;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/3/29
 */
public class MqttUtil {
    /**
     * Topic 通配符
     */
    private static final char[] TOPIC_WILDCARDS = {'#', '+'};

    public static boolean containsTopicWildcards(String topicName) {
        for (char c : TOPIC_WILDCARDS) {
            if (topicName.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }

    public static MqttPublishMessage createPublishMessage(int packetId, String topic, MqttQoS subscribeQos, byte[] payload) {
        return MqttMessageBuilders.publish().payload(payload).qos(subscribeQos).packetId(packetId).topicName(topic).build();
    }

    public static String createClientId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 解析插件配置文件
     */
    public static <T> T getConfig(File yamlFile, String path, Class<T> clazz) {
        ValidateUtils.isTrue(yamlFile.isFile(), "yaml file is not exists!");
        try (FileInputStream fileInputStream = new FileInputStream(yamlFile);) {
            Yaml yaml = new Yaml();
            Object object = yaml.load(fileInputStream);
            String json = JSONObject.toJSONString(object);
            JSONPath jsonPath = JSONPath.of(path);
            JSONReader parser = JSONReader.of(json);
            Object result = jsonPath.extract(parser);
            if (result instanceof JSONObject) {
                return ((JSONObject) result).to(clazz);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
