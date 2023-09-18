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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class KafkaPlugin extends DataPersistPlugin<KafkaPluginConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaPlugin.class);
    private static final String CONFIG_JSON_PATH = "$['plugins']['kafka-bridge'][0]";
    private static StrUtils<MessageNodeInfo> StrUtil = new StrUtils<>();
    private static final Properties KAFKAPROPS = new Properties();
    private BlockingQueue<MessageNodeInfo> queue = new LinkedBlockingQueue<>();
    private KafkaProducer<String, String> producer;
    @Override
    protected KafkaPluginConfig connect(BrokerContext brokerContext) {
        KafkaPluginConfig config = brokerContext.parseConfig(CONFIG_JSON_PATH, KafkaPluginConfig.class);
        if (config == null) {
            LOGGER.error("config maybe error, parse fail!");
            throw new PluginException("start DataPersistRedisPlugin exception");
        }
        this.setConfig(config);
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
    
        // 消费者线程
        Thread consumerThread = new Thread(() -> {
            while (true) {
                try {
                    MessageNodeInfo messageNodeInfo = queue.take();// 从队列中获取数据，如果队列为空则阻塞
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
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        consumerThread.start();
        return config;
    }
    @Override
    protected void listenAndPushMessage(BrokerContext brokerContext, KafkaPluginConfig config) {
        MessageBus messageBus = brokerContext.getMessageBus();
        messageBus.consumer((brokerContext1, publishMessage) -> {
            MessageNodeInfo messageNodeInfo = new MessageNodeInfo(publishMessage);
            try {
                queue.put(messageNodeInfo);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
    @Override
    protected void destroyPlugin() {
        producer.flush();
        producer.close();
    }
}
