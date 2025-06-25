package tech.smartboot.mqtt.plugin.cluster;

import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PathParam;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.mqtt.plugin.cluster.upgrade.BinarySSEUpgrade;
import tech.smartboot.mqtt.plugin.cluster.upgrade.SseEmitter;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.MqttSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version v1.0 6/23/25
 */
@Controller("cluster")
public class ClusterController {

    public static final String HEADER_TOPIC = "topic";
    public static final String HEADER_RETAIN = "retain";
    @Autowired
    private MqttSession mqttSession;

    @Autowired
    private BrokerContext brokerContext;

    private final Map<String, SseEmitter> coreNodes = new ConcurrentHashMap<>();
    private final Map<String, SseEmitter> workerNodes = new ConcurrentHashMap<>();

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
        MqttMessage message = parseMessage(request);
        byte[] bytes = message.toBytes();
        for (Map.Entry<String, SseEmitter> entry : coreNodes.entrySet()) {
            SseEmitter value = entry.getValue();
            value.send(bytes);
        }

        workerNodes.forEach((accessToken, emitter) -> {
            if (token.equals(accessToken)) {
                return;
            }
            emitter.send(bytes);
        });
        //推送给自己
        ClusterPlugin.publishMessageBus(brokerContext, mqttSession, message);
    }

    /**
     * 核心节点推送过来的消息只发生给worker
     *
     * @param request
     */
    @RequestMapping("/put/core")
    public boolean putCoreMessage(HttpRequest request) throws IOException {
        MqttMessage message = parseMessage(request);
        byte[] bytes = message.toBytes();
        workerNodes.forEach((nodeId, emitter) -> emitter.send(bytes));

        //推送给自己
        ClusterPlugin.publishMessageBus(brokerContext, mqttSession, message);
        return true;
    }

    private MqttMessage parseMessage(HttpRequest request) throws IOException {
        String topic = request.getHeader(HEADER_TOPIC);
        String retain = request.getHeader(HEADER_RETAIN);
        byte[] payload = FeatUtils.toByteArray(request.getInputStream());
        MqttMessage message = new MqttMessage();
        message.setTopic(topic);
        message.setPayload(payload);
        message.setRetained(retain != null);
        return message;
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
                if (ClusterPlugin.NODE_TYPE_CORE.equals(nodeType)) {
                    coreNodes.put(accessToken, sseEmitter);
                }
                if (ClusterPlugin.NODE_TYPE_WORKER.equals(nodeType)) {
                    workerNodes.put(accessToken, sseEmitter);
                }
            }

            @Override
            public void destroy() {
                super.destroy();
                coreNodes.remove(accessToken);
                workerNodes.remove(accessToken);
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
