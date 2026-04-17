package tech.smartboot.mqtt.plugin.kafka.storage.store;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class StorePaths {
    private StorePaths() {
    }

    static String encodeTopic(String topic) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(topic.getBytes(StandardCharsets.UTF_8));
    }
}
