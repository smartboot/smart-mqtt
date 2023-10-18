package org.smartboot.mqtt.data.persistence.impl;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.eventbus.messagebus.MessageBus;
import org.smartboot.mqtt.broker.plugin.PluginException;
import org.smartboot.mqtt.data.persistence.DataPersistPlugin;
import org.smartboot.mqtt.data.persistence.config.imp.KafkaPluginConfig;
import org.smartboot.mqtt.data.persistence.nodeinfo.MessageNodeInfo;
import org.smartboot.mqtt.data.persistence.utils.StrUtils;

import java.util.Properties;
import java.util.concurrent.Future;

/**
* @Description: KafkaPlugin插件
 * @Author: learnhope
 * @Date: 2023/9/19
 */
public class KafkaPlugin extends DataPersistPlugin<KafkaPluginConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaPlugin.class);
    private static final String CONFIG_JSON_PATH = "$['plugins']['kafka-bridge'][0]";
    private static StrUtils<MessageNodeInfo> StrUtil = new StrUtils<>();
    private static final Properties KAFKAPROPS = new Properties();
    private KafkaProducer<String, String> producer;
    @Override
    protected KafkaPluginConfig connect(BrokerContext brokerContext) {
        KafkaPluginConfig config = brokerContext.parseConfig(CONFIG_JSON_PATH, KafkaPluginConfig.class);
        if (config == null) {
            LOGGER.error("config maybe error, parse fail!");
            throw new PluginException("start DataPersistKafkaPlugin exception");
        }
        this.setConfig(config);
        // 相关配置
        KAFKAPROPS.put("bootstrap.servers", config.getHost());
        KAFKAPROPS.put("acks", config.getAcks());
        KAFKAPROPS.put("retries", config.getRetries());
        KAFKAPROPS.put("batch.size", config.getBatchSize());
        KAFKAPROPS.put("linger.ms", config.getLingerMs());
        KAFKAPROPS.put("buffer.memory", config.getBuffer());
        KAFKAPROPS.put("key.serializer",
                "org.apache.kafka.common.serialization.IntegerSerializer");
        KAFKAPROPS.put("value.serializer",
                "org.apache.kafka.common.serialization.StringSerializer");
        producer = new KafkaProducer<String, String>(KAFKAPROPS);
        return config;
    }
    @Override
    protected void listenAndPushMessage(BrokerContext brokerContext, KafkaPluginConfig config) {
        MessageBus messageBus = brokerContext.getMessageBus();
        messageBus.consumer(busMessage -> {
            MessageNodeInfo messageNodeInfo = new MessageNodeInfo(busMessage);
            String message = messageNodeInfo.toString();
            // 完成playload信息base64编码
            if (config.isBase64()){
                message = StrUtil.base64(messageNodeInfo);
            }
            // 异步发送消息
            ProducerRecord<String, String> record = new ProducerRecord<>(messageNodeInfo.getTopic(), message);
            Future<RecordMetadata> result = producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception != null) {
                        exception.printStackTrace();
                    }
                }
            });
        });
    }
    @Override
    protected void destroyPlugin() {
        producer.flush();
        producer.close();
    }
}
