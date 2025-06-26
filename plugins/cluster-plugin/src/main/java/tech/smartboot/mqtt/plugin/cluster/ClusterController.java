package tech.smartboot.mqtt.plugin.cluster;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.cluster.upgrade.BinarySSEUpgrade;
import tech.smartboot.mqtt.plugin.cluster.upgrade.SseEmitter;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version v1.0 6/23/25
 */
@Controller("cluster")
public class ClusterController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterController.class);
    public static final String HEADER_TOPIC = "topic";
    public static final String HEADER_RETAIN = "retain";
    @Autowired
    private MqttSession mqttSession;

    @Autowired
    private BrokerContext brokerContext;

    private final Map<String, SseEmitter> coreNodes = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> workerNodes = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        brokerContext.getEventBus().subscribe(ClusterPlugin.CLIENT_DIRECT_TO_CORE_BROKER, (eventType, message) -> {
            byte[] bytes = toBytes(message);
            LOGGER.info("receive cluster message, workerNodes:{}", ClusterController.this.workerNodes.size());
            ClusterController.this.workerNodes.forEach((nodeId, emitter) -> emitter.send(bytes));
        });
    }

    @RequestMapping("/status")
    public boolean status() {
        return true;
    }

    /**
     * 接受到来自worker节点的消息，
     * 推送给集群各节点和直连的worker
     *
     * @param request
     */
    @RequestMapping("/put/worker")
    public void putMessage(HttpRequest request) throws IOException {
        String token = request.getHeader("access_token");
        Message message = parseMessage(request);
        byte[] bytes = toBytes(message);
        for (Map.Entry<String, SseEmitter> entry : coreNodes.entrySet()) {
            SseEmitter value = entry.getValue();
            value.send(bytes);
        }

        workerNodes.forEach((accessToken, emitter) -> {
            if (token.equals(accessToken)) {
                return;
            }
            LOGGER.info("分发消息至集群节点:{}", emitter.getAccessToken());
            emitter.send(bytes);
        });
        //推送给自己
        brokerContext.getMessageBus().publish(mqttSession, message);
        request.getResponse().setHttpStatus(HttpStatus.ACCEPTED);
        request.getResponse().getOutputStream().disableChunked();
    }

    /**
     * 核心节点推送过来的消息只发生给worker
     *
     * @param request
     */
    @RequestMapping("/put/core")
    public void putCoreMessage(HttpRequest request) throws IOException {
        Message message = parseMessage(request);
        byte[] bytes = toBytes(message);
        System.out.println("receive cluster message...");
        workerNodes.forEach((nodeId, emitter) -> emitter.send(bytes));

        //推送给自己
        brokerContext.getMessageBus().publish(mqttSession, message);
        System.out.println("receive cluster done...");
        request.getResponse().setHttpStatus(HttpStatus.ACCEPTED);
        request.getResponse().getOutputStream().disableChunked();
    }

    private Message parseMessage(HttpRequest request) throws IOException {
        String topic = request.getHeader(HEADER_TOPIC);
        ValidateUtils.notBlank(topic, "topic is null");
        String retain = request.getHeader(HEADER_RETAIN);
        byte[] payload = FeatUtils.toByteArray(request.getInputStream());
        return new Message(brokerContext.getOrCreateTopic(topic), MqttQoS.AT_MOST_ONCE, payload, retain != null);
    }

    public byte[] toBytes(Message message) {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream(message.getPayload().length + 32);
        try {
            byteOutputStream.write(BinaryServerSentEventStream.TAG_TOPIC);
            byteOutputStream.write(':');
            byteOutputStream.write(message.getTopic().getTopic().getBytes());
            byteOutputStream.write('\n');

            if (message.isRetained()) {
                byteOutputStream.write(BinaryServerSentEventStream.TAG_RETAIN);
                byteOutputStream.write(':');
                byteOutputStream.write('\n');
            }
            byteOutputStream.write(BinaryServerSentEventStream.TAG_PAYLOAD);
            byteOutputStream.write(':');
            byteOutputStream.write((message.getPayload().length + " ").getBytes());
            byteOutputStream.write(message.getPayload());
            byteOutputStream.write('\n');
            byteOutputStream.write('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteOutputStream.toByteArray();
    }

    /**
     * work节点订阅集群消息
     */
    @RequestMapping("/subscribe/:nodeType/:access_token")
    public void subscribeMessage(HttpRequest request, @PathParam("nodeType") String nodeType, @PathParam("access_token") String accessToken) throws IOException {
        request.upgrade(new BinarySSEUpgrade() {
            @Override
            public void onOpen(SseEmitter sseEmitter) throws IOException {
                sseEmitter.setAccessToken(accessToken);
                SseEmitter old = null;
                if (ClusterPlugin.NODE_TYPE_CORE.equals(nodeType)) {
                    LOGGER.info("接收来自core节点的订阅:{}", accessToken);
                    old = coreNodes.put(accessToken, sseEmitter);
                } else if (ClusterPlugin.NODE_TYPE_WORKER.equals(nodeType)) {
                    LOGGER.info("接收来自worker节点的订阅:{}", accessToken);
                    old = workerNodes.put(accessToken, sseEmitter);
                }
                if (old != null) {
                    LOGGER.info("移除旧节点:{}", old.getAccessToken());
                    old.complete();
                }
            }

            @Override
            public void destroy() {
                super.destroy();
                coreNodes.remove(accessToken);
                workerNodes.remove(accessToken);
                LOGGER.info("移除节点:{}", accessToken);
                new Throwable().printStackTrace();
            }
        });
    }

    public void setMqttSession(MqttSession mqttSession) {
        this.mqttSession = mqttSession;
    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }
}
