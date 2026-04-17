# Kafka Storage Plugin

`kafka-storage-plugin` 为 smart-mqtt 提供本地可靠消息持久化能力，并暴露基于 `smart-socket` 实现的 Kafka 协议接入端口。

## 核心能力

- 接收 MQTT Broker 消息总线中的输入消息，并落盘到按 topic/partition 划分的追加日志
- 提供 Kafka Producer / Consumer 可直接连接的二进制协议入口
- 支持消息分区、offset 查询、offset 提交与恢复
- 支持存储路径、分段大小、容量限制、保留时长、flush 策略等配置
- 提供周期性指标日志，便于运维观测与问题排查

## 安装

1. 编译插件：

```bash
cd plugins
mvn -pl kafka-storage-plugin -am package
```

2. 将生成的 `kafka-storage-plugin-<version>.jar` 放入 `SMART_MQTT_PLUGINS` 目录。

3. 在插件存储目录中创建或修改 `plugin.yaml`。

## 关键配置

```yaml
kafka:
  port: 9092
  advertised_host: 127.0.0.1
  advertised_port: 9092

storage:
  data_path: data
  default_partition_count: 3
  retention_bytes: 1073741824
  retention_hours: 168
  flush_interval_ms: 1000
```

## 使用说明

### MQTT 消息持久化

插件启动后会自动订阅 Broker 消息总线。凡是进入 Broker 的普通消息，都会按配置落盘，并自动映射到 Kafka topic。

### Kafka 生产者

默认兼容非事务和幂等初始化握手的 Java Producer：

```java
Properties props = new Properties();
props.put("bootstrap.servers", "127.0.0.1:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
KafkaProducer<String, String> producer = new KafkaProducer<>(props);
producer.send(new ProducerRecord<>("telemetry/device", "k1", "hello"));
producer.flush();
```

### Kafka 消费者

当前版本重点覆盖标准 `assign` / `seek` / `poll` / `commitSync` 流程：

```java
Properties props = new Properties();
props.put("bootstrap.servers", "127.0.0.1:9092");
props.put("group.id", "demo-group");
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("auto.offset.reset", "earliest");
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
TopicPartition tp = new TopicPartition("telemetry/device", 0);
consumer.assign(Collections.singletonList(tp));
consumer.poll(Duration.ofSeconds(1));
consumer.commitSync();
```

## 指标日志

插件会按 `metrics.log_interval_ms` 周期输出如下指标：

- `mqtt_messages`
- `kafka_produced_messages`
- `fetched_messages`
- `committed_offsets`
- `stored_bytes`
- `active_topics`
- `active_consumer_groups`

## 限制说明

- 当前版本聚焦单节点本地持久化与 Kafka 兼容接入，不包含跨节点副本复制
- Kafka Consumer Group 的自动 rebalance 协议不在本次实现范围内，推荐使用标准客户端的手动分配模式
