package tech.smartboot.mqtt.plugin.cluster;

import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.mqtt.client.MqttClient;
import tech.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.common.message.payload.MqttConnectPayload;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.bus.EventBusConsumer;
import tech.smartboot.mqtt.plugin.spec.bus.EventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;
import tech.smartboot.mqtt.plugin.spec.bus.MessageBusConsumer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 三刀
 * @version v1.0 6/23/25
 */
public class ClusterPlugin extends Plugin {
    private static final MqttMessage SHUTDOWN_MESSAGE = new MqttMessage();
    public static final String NODE_TYPE_CORE = "core";
    public static final String NODE_TYPE_WORKER = "worker";

    private static final int QUEUE_POLICY_DISCARD_NEWEST = 0;
    private static final int QUEUE_POLICY_DISCARD_OLDEST = 1;
    /**
     * 集群内其他协调节点建立的连接
     */
    private final Map<MqttSession, String> coreSessions = new ConcurrentHashMap<>();

    private final Map<MqttSession, String> workerSessions = new ConcurrentHashMap<>();

    /**
     * 与当前节点直连的节点
     */
    private final Map<String, MqttClient> connectedNodes = new ConcurrentHashMap<>();

    private MqttClient mqttClient;
    private boolean enabled = true;
    private ArrayBlockingQueue<MqttMessage> queue;
    private int queuePolicy;

    private List<HttpClient> sseClients = new ArrayList<>();

    private List<HttpClient> publishClients = new ArrayList<>();
    private HttpServer httpServer;
    private String clientId;

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        PluginConfig pluginConfig = loadPluginConfig(PluginConfig.class);

        // 队列策略
        if (QUEUE_POLICY_DISCARD_OLDEST == pluginConfig.getQueuePolicy()) {
            queuePolicy = QUEUE_POLICY_DISCARD_OLDEST;
        } else {
            queuePolicy = QUEUE_POLICY_DISCARD_NEWEST;
        }

        // 队列长度
        int length = pluginConfig.getQueueLength();
        if (length < 1) {
            length = 1024;
        } else if (length > Short.MAX_VALUE) {
            length = Short.MAX_VALUE;
        }
        queue = new ArrayBlockingQueue<>(length);


        //启动核心节点服务监听
        if (pluginConfig.isCore()) {
            httpServer = FeatCloud.cloudServer(cloudOptions -> cloudOptions.host(pluginConfig.getHost()).port(pluginConfig.getPort())).listen();
        }

        for (String cluster : pluginConfig.getClusters()) {
            initClusterClient(brokerContext, cluster, pluginConfig.isCore());
        }


        initClusterMessageConsumer(brokerContext);

    }

    private void initClusterMessageConsumer(BrokerContext brokerContext) {
        clientId = "internal-" + UUID.randomUUID();
        final String userName = UUID.randomUUID().toString();
        final byte[] password = UUID.randomUUID().toString().getBytes();

        //动态注入内部Client认证策略
        brokerContext.getEventBus().subscribe(EventType.CONNECT, new EventBusConsumer<EventObject<MqttConnectMessage>>() {
            @Override
            public void consumer(EventType<EventObject<MqttConnectMessage>> eventType, EventObject<MqttConnectMessage> object) {
                MqttSession session = object.getSession();
                if (session.isAuthorized() || !clientId.equals(object.getObject().getPayload().clientId())) {
                    return;
                }
                MqttConnectPayload payload = object.getObject().getPayload();
                if (userName.equals(payload.userName()) && Arrays.equals(password, payload.passwordInBytes())) {
                    session.setAuthorized(true);
                } else {
                    MqttSession.connFailAck(MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, session);
                }
            }

            @Override
            public boolean enable() {
                return enabled;
            }
        });

        //启动内部MqttClient，推送从集群接收到的数据
        mqttClient = new MqttClient("0.0.0.0", brokerContext.Options().getPort(), options -> options.setClientId(clientId).setUserName(userName).setPassword(password));
        mqttClient.connect();
        new Thread(() -> {
            while (enabled) {
                try {
                    MqttMessage message = queue.take();
                    do {
                        if (SHUTDOWN_MESSAGE == message) {
                            break;
                        }
                        mqttClient.publish(message.getTopic(), MqttQoS.AT_MOST_ONCE, message.getPayload(), message.isRetained(), false);
                    } while ((message = queue.poll()) != null);
                    mqttClient.flush();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "cluster-plugin-client").start();
    }

    @Override
    protected void destroyPlugin() {
        enabled = false;
        //停止核心节点服务
        if (httpServer != null) {
            httpServer.shutdown();
        }

        //中断集群数据监听
        sseClients.forEach(HttpClient::close);

        try {
            while (queue.poll() != null) ;
            queue.put(SHUTDOWN_MESSAGE);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        mqttClient.disconnect();
    }


    private void initClusterClient(BrokerContext brokerContext, String nodeId, boolean core) {
        HttpClient httpClient = new HttpClient(nodeId);
        publishClients.add(httpClient);
        //将消息总线中的消息发送给集群
        brokerContext.getMessageBus().consumer(new MessageBusConsumer() {
            @Override
            public void consume(MqttSession session, Message message) {
                //忽略来自集群的消息
                if (session.getClientId().equals(clientId)) {
                    return;
                }
                //分发给各节点

                if (core) {
                    for (HttpClient publishClient : publishClients) {
                        publishClient.post("/put/core").header(header -> header.setContentLength(message.getPayload().length).set(ClusterController.HEADER_TOPIC, message.getTopic().getTopic())).body(requestBody -> requestBody.write(message.getPayload())).submit();
                    }

                } else {
                    httpClient.post("/put/work");
                }
            }

            @Override
            public boolean enable() {
                return enabled;
            }
        });

        HttpClient sseClient = new HttpClient(nodeId);
        sseClients.add(sseClient);
        //订阅集群推送过来的消息，并投递至总线
        sseClient.post().onResponseBody(new BinaryServerSentEventStream() {

            @Override
            public void onEvent(HttpResponse httpResponse, MqttMessage event) {
                if (!enabled) {
                    return;
                }
                if (queuePolicy == QUEUE_POLICY_DISCARD_NEWEST) {
                    queue.offer(event);
                } else {
                    while (!queue.offer(event)) {
                        queue.poll();
                    }
                }
            }
        }).submit();
    }


    @Override
    public String getVersion() {
        return Options.VERSION;
    }

    @Override
    public String getVendor() {
        return Options.VENDOR;
    }

    @Override
    public String pluginName() {
        return "cluster-plugin";
    }
}
