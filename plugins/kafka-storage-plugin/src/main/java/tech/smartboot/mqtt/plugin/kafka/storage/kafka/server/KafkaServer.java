package tech.smartboot.mqtt.plugin.kafka.storage.kafka.server;

import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicIdPartition;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.message.ApiVersionsRequestData;
import org.apache.kafka.common.message.ApiVersionsResponseData;
import org.apache.kafka.common.message.FetchResponseData;
import org.apache.kafka.common.message.FindCoordinatorResponseData;
import org.apache.kafka.common.message.InitProducerIdResponseData;
import org.apache.kafka.common.message.ListOffsetsResponseData;
import org.apache.kafka.common.message.MetadataResponseData;
import org.apache.kafka.common.message.OffsetCommitResponseData;
import org.apache.kafka.common.message.ProduceResponseData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.protocol.Message;
import org.apache.kafka.common.protocol.MessageUtil;
import org.apache.kafka.common.record.CompressionType;
import org.apache.kafka.common.record.MemoryRecords;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.record.SimpleRecord;
import org.apache.kafka.common.requests.ApiVersionsRequest;
import org.apache.kafka.common.requests.ApiVersionsResponse;
import org.apache.kafka.common.requests.FetchRequest;
import org.apache.kafka.common.requests.FetchResponse;
import org.apache.kafka.common.requests.FindCoordinatorRequest;
import org.apache.kafka.common.requests.FindCoordinatorResponse;
import org.apache.kafka.common.requests.InitProducerIdRequest;
import org.apache.kafka.common.requests.ListOffsetsRequest;
import org.apache.kafka.common.requests.MetadataRequest;
import org.apache.kafka.common.requests.MetadataResponse;
import org.apache.kafka.common.requests.OffsetCommitRequest;
import org.apache.kafka.common.requests.OffsetFetchRequest;
import org.apache.kafka.common.requests.OffsetFetchResponse;
import org.apache.kafka.common.requests.ProduceRequest;
import org.apache.kafka.common.requests.ProduceResponse;
import org.apache.kafka.common.utils.Utils;
import org.smartboot.socket.transport.AioQuickServer;
import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;
import tech.smartboot.mqtt.plugin.kafka.storage.config.PluginConfig;
import tech.smartboot.mqtt.plugin.kafka.storage.kafka.protocol.KafkaProtocol;
import tech.smartboot.mqtt.plugin.kafka.storage.kafka.protocol.KafkaRequestFrame;
import tech.smartboot.mqtt.plugin.kafka.storage.metrics.KafkaStorageMetrics;
import tech.smartboot.mqtt.plugin.kafka.storage.store.ConsumerOffsetStore;
import tech.smartboot.mqtt.plugin.kafka.storage.store.PartitionLog;
import tech.smartboot.mqtt.plugin.kafka.storage.store.PersistentMessageStore;
import tech.smartboot.mqtt.plugin.kafka.storage.store.StoredMessage;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于 smart-socket 的 Kafka 协议接入层。
 */
public class KafkaServer {
    private static final int MAX_FETCH_RECORDS = 256;

    private final PluginConfig.KafkaConfig kafkaConfig;
    private final BrokerContext brokerContext;
    private final PersistentMessageStore store;
    private final KafkaStorageMetrics metrics;
    private final Node node;
    private final Map<ApiKeys, Short> supportedVersions = new ConcurrentHashMap<>();
    private final AtomicLong producerIdSequence = new AtomicLong(1);

    private AioQuickServer server;

    public KafkaServer(PluginConfig.KafkaConfig kafkaConfig,
                       BrokerContext brokerContext,
                       PersistentMessageStore store,
                       KafkaStorageMetrics metrics) {
        this.kafkaConfig = kafkaConfig;
        this.brokerContext = brokerContext;
        this.store = store;
        this.metrics = metrics;
        this.node = new Node(kafkaConfig.getBrokerId(), kafkaConfig.getAdvertisedHost(), kafkaConfig.getAdvertisedPort());
        initSupportedVersions();
    }

    public void start() throws IOException {
        KafkaMessageProcessor processor = new KafkaMessageProcessor(this);
        server = new AioQuickServer(kafkaConfig.getHost(), kafkaConfig.getPort(), new KafkaProtocol(kafkaConfig.getRequestMaxBytes()), processor);
        server.start(brokerContext.Options().getChannelGroup());
    }

    public void shutdown() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void handle(AioSession session, KafkaRequestFrame frame) {
        try {
            switch (frame.getApiKey()) {
                case API_VERSIONS:
                    handleApiVersions(session, frame);
                    break;
                case METADATA:
                    handleMetadata(session, frame);
                    break;
                case INIT_PRODUCER_ID:
                    handleInitProducerId(session, frame);
                    break;
                case PRODUCE:
                    handleProduce(session, frame);
                    break;
                case FETCH:
                    handleFetch(session, frame);
                    break;
                case LIST_OFFSETS:
                    handleListOffsets(session, frame);
                    break;
                case FIND_COORDINATOR:
                    handleFindCoordinator(session, frame);
                    break;
                case OFFSET_COMMIT:
                    handleOffsetCommit(session, frame);
                    break;
                case OFFSET_FETCH:
                    handleOffsetFetch(session, frame);
                    break;
                default:
                    metrics.markError();
                    session.close();
                    break;
            }
        } catch (Throwable e) {
            metrics.markError();
            e.printStackTrace();
            session.close();
        }
    }

    private void handleApiVersions(AioSession session, KafkaRequestFrame frame) throws IOException {
        short supportedVersion = versionOf(ApiKeys.API_VERSIONS);
        if (frame.getApiVersion() > supportedVersion) {
            ApiVersionsResponseData data = new ApiVersionsResponseData()
                    .setErrorCode(Errors.UNSUPPORTED_VERSION.code());
            ApiVersionsResponseData.ApiVersionCollection apiKeys = new ApiVersionsResponseData.ApiVersionCollection();
            apiKeys.add(new ApiVersionsResponseData.ApiVersion()
                    .setApiKey(ApiKeys.API_VERSIONS.id)
                    .setMinVersion((short) 0)
                    .setMaxVersion(supportedVersion));
            data.setApiKeys(apiKeys);
            sendResponse(session, frame, (short) 0, data);
            return;
        }
        ApiVersionsRequest request = ApiVersionsRequest.parse(frame.getBody(), frame.getApiVersion());
        if (!request.isValid()) {
            sendResponse(session, frame, frame.getApiVersion(),
                    request.getErrorResponse(0, Errors.INVALID_REQUEST.exception()).data());
            return;
        }
        ApiVersionsResponseData.ApiVersionCollection apiKeys = new ApiVersionsResponseData.ApiVersionCollection();
        for (Map.Entry<ApiKeys, Short> entry : supportedVersions.entrySet()) {
            apiKeys.add(new ApiVersionsResponseData.ApiVersion()
                    .setApiKey(entry.getKey().id)
                    .setMinVersion(entry.getValue())
                    .setMaxVersion(entry.getValue()));
        }
        ApiVersionsResponse response = ApiVersionsResponse.createApiVersionsResponse(0, apiKeys);
        sendResponse(session, frame, frame.getApiVersion(), response.data());
    }

    private void handleMetadata(AioSession session, KafkaRequestFrame frame) throws IOException {
        MetadataRequest request = MetadataRequest.parse(frame.getBody(), frame.getApiVersion());
        List<String> topics = request.topics();
        List<String> resultTopics = topics == null ? store.topicNames() : new ArrayList<String>(topics);
        if (topics != null && request.allowAutoTopicCreation() && brokerAutoCreateEnabled()) {
            for (String topic : topics) {
                store.ensureTopic(topic);
            }
            resultTopics = new ArrayList<String>(topics);
        }

        List<MetadataResponseData.MetadataResponseTopic> topicResponses = new ArrayList<>();
        for (String topic : resultTopics) {
            if (!store.containsTopic(topic)) {
                topicResponses.add(new MetadataResponseData.MetadataResponseTopic()
                        .setName(topic)
                        .setErrorCode(Errors.UNKNOWN_TOPIC_OR_PARTITION.code())
                        .setPartitions(Collections.<MetadataResponseData.MetadataResponsePartition>emptyList()));
                continue;
            }
            int partitions = store.partitionCount(topic);
            List<MetadataResponseData.MetadataResponsePartition> partitionResponses = new ArrayList<>();
            for (int partition = 0; partition < partitions; partition++) {
                partitionResponses.add(new MetadataResponseData.MetadataResponsePartition()
                        .setPartitionIndex(partition)
                        .setErrorCode(Errors.NONE.code())
                        .setLeaderId(node.id())
                        .setLeaderEpoch(0)
                        .setReplicaNodes(Collections.singletonList(node.id()))
                        .setIsrNodes(Collections.singletonList(node.id()))
                        .setOfflineReplicas(Collections.<Integer>emptyList()));
            }
            topicResponses.add(new MetadataResponseData.MetadataResponseTopic()
                    .setName(topic)
                    .setErrorCode(Errors.NONE.code())
                    .setIsInternal(false)
                    .setPartitions(partitionResponses));
        }
        MetadataResponse response = MetadataResponse.prepareResponse(
                frame.getApiVersion(),
                0,
                Collections.singletonList(node),
                kafkaConfig.getClusterId(),
                node.id(),
                topicResponses,
                MetadataResponse.AUTHORIZED_OPERATIONS_OMITTED);
        sendResponse(session, frame, frame.getApiVersion(), response.data());
    }

    private void handleInitProducerId(AioSession session, KafkaRequestFrame frame) throws IOException {
        InitProducerIdRequest.parse(frame.getBody(), frame.getApiVersion());
        InitProducerIdResponseData response = new InitProducerIdResponseData()
                .setThrottleTimeMs(0)
                .setErrorCode(Errors.NONE.code())
                .setProducerId(producerIdSequence.getAndIncrement())
                .setProducerEpoch((short) 0);
        sendResponse(session, frame, frame.getApiVersion(), response);
    }

    private void handleProduce(AioSession session, KafkaRequestFrame frame) throws IOException {
        ProduceRequest request = ProduceRequest.parse(frame.getBody(), frame.getApiVersion());
        ProduceResponseData response = new ProduceResponseData().setThrottleTimeMs(0);
        for (org.apache.kafka.common.message.ProduceRequestData.TopicProduceData topicData : request.data().topicData()) {
            String topic = topicData.name();
            store.ensureTopic(topic);
            ProduceResponseData.TopicProduceResponse topicResponse = new ProduceResponseData.TopicProduceResponse().setName(topic);
            response.responses().add(topicResponse);
            for (org.apache.kafka.common.message.ProduceRequestData.PartitionProduceData partitionData : topicData.partitionData()) {
                int partition = partitionData.index();
                ProduceResponseData.PartitionProduceResponse partitionResponse = new ProduceResponseData.PartitionProduceResponse()
                        .setIndex(partition)
                        .setErrorCode(Errors.NONE.code())
                        .setRecordErrors(Collections.<ProduceResponseData.BatchIndexAndErrorMessage>emptyList())
                        .setErrorMessage(null);
                if (partition < 0 || partition >= store.partitionCount(topic)) {
                    partitionResponse
                            .setErrorCode(Errors.UNKNOWN_TOPIC_OR_PARTITION.code())
                            .setBaseOffset(ProduceResponse.INVALID_OFFSET)
                            .setLogStartOffset(ProduceResponse.INVALID_OFFSET)
                            .setLogAppendTimeMs(RecordBatch.NO_TIMESTAMP);
                    topicResponse.partitionResponses().add(partitionResponse);
                    continue;
                }

                long firstOffset = ProduceResponse.INVALID_OFFSET;
                if (partitionData.records() instanceof org.apache.kafka.common.record.Records) {
                    org.apache.kafka.common.record.Records records = (org.apache.kafka.common.record.Records) partitionData.records();
                    for (org.apache.kafka.common.record.RecordBatch batch : records.batches()) {
                        for (org.apache.kafka.common.record.Record record : batch) {
                            byte[] key = record.key() == null ? null : Utils.toArray(record.key());
                            byte[] value = record.value() == null ? null : Utils.toArray(record.value());
                            long timestamp = record.timestamp() == RecordBatch.NO_TIMESTAMP ? System.currentTimeMillis() : record.timestamp();
                            StoredMessage appended = store.appendKafka(topic, partition, key, value, timestamp, frame.getHeader().clientId());
                            if (firstOffset == ProduceResponse.INVALID_OFFSET) {
                                firstOffset = appended.getOffset();
                            }
                        }
                    }
                }
                partitionResponse
                        .setBaseOffset(firstOffset)
                        .setLogStartOffset(store.logStartOffset(topic, partition))
                        .setLogAppendTimeMs(RecordBatch.NO_TIMESTAMP);
                topicResponse.partitionResponses().add(partitionResponse);
            }
        }
        if (request.acks() != 0) {
            sendResponse(session, frame, frame.getApiVersion(), response);
        }
    }

    private void handleFetch(AioSession session, KafkaRequestFrame frame) throws IOException {
        FetchRequest request = FetchRequest.parse(frame.getBody(), frame.getApiVersion());
        LinkedHashMap<TopicIdPartition, FetchResponseData.PartitionData> responseData = new LinkedHashMap<>();
        for (Map.Entry<TopicIdPartition, FetchRequest.PartitionData> entry : request.fetchData(Collections.<Uuid, String>emptyMap()).entrySet()) {
            String topic = entry.getKey().topicPartition().topic();
            int partition = entry.getKey().topicPartition().partition();
            if (!store.containsTopic(topic) || partition < 0 || partition >= store.partitionCount(topic)) {
                responseData.put(entry.getKey(), FetchResponse.partitionResponse(partition, Errors.UNKNOWN_TOPIC_OR_PARTITION));
                continue;
            }
            PartitionLog.FetchResult result = store.fetch(topic, partition, entry.getValue().fetchOffset, entry.getValue().maxBytes, MAX_FETCH_RECORDS);
            MemoryRecords records;
            if (result.getMessages().isEmpty()) {
                records = MemoryRecords.EMPTY;
            } else {
                StoredMessage first = result.getMessages().get(0);
                List<SimpleRecord> simpleRecords = new ArrayList<>();
                for (StoredMessage message : result.getMessages()) {
                    simpleRecords.add(new SimpleRecord(message.getTimestamp(), message.getKey(), message.getValue()));
                }
                records = MemoryRecords.withRecords(first.getOffset(), CompressionType.NONE, simpleRecords.toArray(new SimpleRecord[0]));
            }
            FetchResponseData.PartitionData partitionData = new FetchResponseData.PartitionData()
                    .setPartitionIndex(partition)
                    .setErrorCode(Errors.NONE.code())
                    .setHighWatermark(result.getHighWatermark())
                    .setLastStableOffset(result.getHighWatermark())
                    .setLogStartOffset(result.getLogStartOffset())
                    .setRecords(records);
            responseData.put(entry.getKey(), partitionData);
        }
        FetchResponse response = FetchResponse.of(Errors.NONE, 0, request.metadata().sessionId(), responseData);
        sendResponse(session, frame, frame.getApiVersion(), response.data());
    }

    private void handleListOffsets(AioSession session, KafkaRequestFrame frame) throws IOException {
        ListOffsetsRequest request = ListOffsetsRequest.parse(frame.getBody(), frame.getApiVersion());
        List<ListOffsetsResponseData.ListOffsetsTopicResponse> topics = new ArrayList<>();
        for (org.apache.kafka.common.message.ListOffsetsRequestData.ListOffsetsTopic requestTopic : request.topics()) {
            ListOffsetsResponseData.ListOffsetsTopicResponse topicResponse = new ListOffsetsResponseData.ListOffsetsTopicResponse()
                    .setName(requestTopic.name());
            topics.add(topicResponse);
            for (org.apache.kafka.common.message.ListOffsetsRequestData.ListOffsetsPartition requestPartition : requestTopic.partitions()) {
                ListOffsetsResponseData.ListOffsetsPartitionResponse partitionResponse = new ListOffsetsResponseData.ListOffsetsPartitionResponse()
                        .setPartitionIndex(requestPartition.partitionIndex());
                if (!store.containsTopic(requestTopic.name()) || requestPartition.partitionIndex() < 0 || requestPartition.partitionIndex() >= store.partitionCount(requestTopic.name())) {
                    partitionResponse
                            .setErrorCode(Errors.UNKNOWN_TOPIC_OR_PARTITION.code())
                            .setOffset(-1)
                            .setTimestamp(-1);
                    topicResponse.partitions().add(partitionResponse);
                    continue;
                }
                PartitionLog.OffsetLookupResult result = store.lookupOffset(requestTopic.name(), requestPartition.partitionIndex(), requestPartition.timestamp());
                partitionResponse
                        .setErrorCode(Errors.NONE.code())
                        .setOffset(result.getOffset())
                        .setTimestamp(result.getTimestamp());
                topicResponse.partitions().add(partitionResponse);
            }
        }
        ListOffsetsResponseData response = new ListOffsetsResponseData()
                .setThrottleTimeMs(0)
                .setTopics(topics);
        sendResponse(session, frame, frame.getApiVersion(), response);
    }

    private void handleFindCoordinator(AioSession session, KafkaRequestFrame frame) throws IOException {
        FindCoordinatorRequest request = FindCoordinatorRequest.parse(frame.getBody(), frame.getApiVersion());
        FindCoordinatorResponse response;
        if (frame.getApiVersion() < FindCoordinatorRequest.MIN_BATCHED_VERSION) {
            response = FindCoordinatorResponse.prepareOldResponse(Errors.NONE, node);
        } else {
            List<String> keys = request.data().coordinatorKeys().isEmpty()
                    ? Collections.singletonList(request.data().key())
                    : request.data().coordinatorKeys();
            response = FindCoordinatorResponse.prepareResponse(Errors.NONE, keys.get(0), node);
        }
        sendResponse(session, frame, frame.getApiVersion(), response.data());
    }

    private void handleOffsetCommit(AioSession session, KafkaRequestFrame frame) throws IOException {
        OffsetCommitRequest request = OffsetCommitRequest.parse(frame.getBody(), frame.getApiVersion());
        OffsetCommitResponseData response = new OffsetCommitResponseData().setThrottleTimeMs(0);
        for (org.apache.kafka.common.message.OffsetCommitRequestData.OffsetCommitRequestTopic requestTopic : request.data().topics()) {
            OffsetCommitResponseData.OffsetCommitResponseTopic topicResponse = new OffsetCommitResponseData.OffsetCommitResponseTopic()
                    .setName(requestTopic.name());
            response.topics().add(topicResponse);
            for (org.apache.kafka.common.message.OffsetCommitRequestData.OffsetCommitRequestPartition requestPartition : requestTopic.partitions()) {
                store.commitOffset(request.data().groupId(), requestTopic.name(), requestPartition.partitionIndex(), requestPartition.committedOffset(), requestPartition.committedMetadata());
                topicResponse.partitions().add(new OffsetCommitResponseData.OffsetCommitResponsePartition()
                        .setPartitionIndex(requestPartition.partitionIndex())
                        .setErrorCode(Errors.NONE.code()));
            }
        }
        sendResponse(session, frame, frame.getApiVersion(), response);
    }

    private void handleOffsetFetch(AioSession session, KafkaRequestFrame frame) throws IOException {
        OffsetFetchRequest request = OffsetFetchRequest.parse(frame.getBody(), frame.getApiVersion());
        OffsetFetchResponse response;
        if (request.partitions() == null) {
            Map<TopicPartition, OffsetFetchResponse.PartitionData> partitionData = new LinkedHashMap<>();
            for (Map.Entry<ConsumerOffsetStore.TopicPartitionKey, ConsumerOffsetStore.OffsetInfo> entry : store.listCommittedOffsets(request.groupId()).entrySet()) {
                ConsumerOffsetStore.OffsetInfo info = entry.getValue();
                partitionData.put(new TopicPartition(entry.getKey().getTopic(), entry.getKey().getPartition()),
                        new OffsetFetchResponse.PartitionData(info.getOffset(), java.util.Optional.empty(), info.getMetadata(), Errors.NONE));
            }
            response = new OffsetFetchResponse(Errors.NONE, partitionData);
        } else {
            Map<TopicPartition, OffsetFetchResponse.PartitionData> partitionData = new LinkedHashMap<>();
            for (TopicPartition topicPartition : request.partitions()) {
                ConsumerOffsetStore.OffsetInfo info = store.readCommittedOffset(request.groupId(), topicPartition.topic(), topicPartition.partition());
                if (info == null) {
                    partitionData.put(topicPartition,
                            new OffsetFetchResponse.PartitionData(OffsetFetchResponse.INVALID_OFFSET, java.util.Optional.empty(), OffsetFetchResponse.NO_METADATA, Errors.NONE));
                } else {
                    partitionData.put(topicPartition,
                            new OffsetFetchResponse.PartitionData(info.getOffset(), java.util.Optional.empty(), info.getMetadata(), Errors.NONE));
                }
            }
            response = new OffsetFetchResponse(Errors.NONE, partitionData);
        }
        sendResponse(session, frame, frame.getApiVersion(), response.data());
    }

    private void sendResponse(AioSession session, KafkaRequestFrame frame, short responseVersion, Message message) throws IOException {
        short responseHeaderVersion = frame.getApiKey().responseHeaderVersion(responseVersion);
        org.apache.kafka.common.message.ResponseHeaderData header = new org.apache.kafka.common.message.ResponseHeaderData()
                .setCorrelationId(frame.getHeader().correlationId());
        ByteBuffer headerBuffer = MessageUtil.toByteBuffer(header, responseHeaderVersion);
        ByteBuffer bodyBuffer = MessageUtil.toByteBuffer(message, responseVersion);
        WriteBuffer writeBuffer = session.writeBuffer();
        int length = headerBuffer.remaining() + bodyBuffer.remaining();
        writeBuffer.writeInt(length);
        writeBuffer.write(headerBuffer.array(), headerBuffer.position(), headerBuffer.remaining());
        writeBuffer.write(bodyBuffer.array(), bodyBuffer.position(), bodyBuffer.remaining());
        writeBuffer.flush();
    }

    private short versionOf(ApiKeys apiKey) {
        return supportedVersions.get(apiKey);
    }

    private boolean brokerAutoCreateEnabled() {
        return true;
    }

    private void initSupportedVersions() {
        supportedVersions.put(ApiKeys.API_VERSIONS, (short) 3);
        supportedVersions.put(ApiKeys.METADATA, (short) 4);
        supportedVersions.put(ApiKeys.INIT_PRODUCER_ID, (short) 4);
        supportedVersions.put(ApiKeys.PRODUCE, (short) 7);
        supportedVersions.put(ApiKeys.FETCH, (short) 4);
        supportedVersions.put(ApiKeys.LIST_OFFSETS, (short) 2);
        supportedVersions.put(ApiKeys.FIND_COORDINATOR, (short) 3);
        supportedVersions.put(ApiKeys.OFFSET_COMMIT, (short) 3);
        supportedVersions.put(ApiKeys.OFFSET_FETCH, (short) 3);
    }
}
