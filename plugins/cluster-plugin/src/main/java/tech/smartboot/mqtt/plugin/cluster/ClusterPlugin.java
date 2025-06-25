package tech.smartboot.mqtt.plugin.cluster;

import org.smartboot.socket.timer.HashedWheelTimer;
import tech.smartboot.feat.cloud.FeatCloud;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpServer;
import tech.smartboot.mqtt.common.AsyncTask;
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
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author 三刀
 * @version v1.0 6/23/25
 */
public class ClusterPlugin extends Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterPlugin.class);
    private static final Message SHUTDOWN_MESSAGE = new Message(null, null, null, false);
    public static final String NODE_TYPE_CORE = "core";
    public static final String NODE_TYPE_WORKER = "worker";

    private static final int QUEUE_POLICY_DISCARD_NEWEST = 0;
    private static final int QUEUE_POLICY_DISCARD_OLDEST = 1;


    private boolean enabled = true;
    private ArrayBlockingQueue<Message> queue;
    private int queuePolicy;

    private HttpServer httpServer;
    private final String clientId = "internal-" + System.nanoTime();
    final String userName = UUID.randomUUID().toString().substring(0, 16);
    final byte[] password = UUID.randomUUID().toString().getBytes();
    private final List<ClientUnit> clients = new ArrayList<>();
    private final HashedWheelTimer timer = new HashedWheelTimer(r -> new Thread(r, "cluster-plugin-health-checker"));
    private BrokerContext brokerContext;
    private ClientUnit workerClient;
    private final ClusterMqttSession mqttSession = new ClusterMqttSession(clientId);
    private static final String ACCESS_TOKEN = UUID.randomUUID().toString();

    @Override
    protected void initPlugin(BrokerContext brokerContext) throws Throwable {
        this.brokerContext = brokerContext;
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


        new Thread(() -> {
            while (enabled) {
                try {
                    Message message = queue.take();
                    do {
                        if (SHUTDOWN_MESSAGE == message) {
                            break;
                        }
                        brokerContext.getMessageBus().publish(mqttSession, message);
                    } while ((message = queue.poll()) != null);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "cluster-plugin-client").start();

        //启动核心节点服务监听
        if (pluginConfig.isCore()) {
            httpServer = FeatCloud.cloudServer(cloudOptions -> cloudOptions.registerBean("mqttSession", mqttSession).registerBean("brokerContext", brokerContext).host(pluginConfig.getHost()).port(pluginConfig.getPort()).debug(true)).listen();
        }

        if (FeatUtils.isNotEmpty(pluginConfig.getClusters())) {
            for (String cluster : pluginConfig.getClusters()) {
                clients.add(new ClientUnit(cluster));
            }
        }
        initClusterClient(brokerContext, pluginConfig.isCore());


        timer.scheduleWithFixedDelay(new AsyncTask() {
            @Override
            public void execute() {
                clients.forEach(clientUnit -> {
                    if (clientUnit.checkPending) {
                        LOGGER.info("check pending message for {}", clientUnit.baseURL);
                        return;
                    }
                    if (clientUnit.httpEnable) {
                        if (workerClient == null) {
                            workerClient = clientUnit;
                            initSSE(clientUnit, pluginConfig.isCore());
                        } else if (!workerClient.sseEnable) { // 释放workerClient，重新分配
                            workerClient.sseClient.close();
                            workerClient = null;
                        }
                        return;
                    }
                    //release old client
                    if (clientUnit.httpClient != null) {
                        clientUnit.httpClient.close();
                        clientUnit.httpClient = null;
                    }
                    clientUnit.httpClient = new HttpClient(clientUnit.baseURL);
                    clientUnit.httpClient.options().connectTimeout(5000).group(brokerContext.Options().getChannelGroup());
                    clientUnit.checkPending = true;
                    clientUnit.httpClient.get("/cluster/status").onSuccess(httpResponse -> {
                        clientUnit.httpEnable = true;
                        clientUnit.checkPending = false;

                    }).onFailure(throwable -> {
                        clientUnit.httpEnable = false;
                        clientUnit.checkPending = false;
                        LOGGER.error("check node status error", throwable);
                    }).submit();
                });
            }
        }, 1, TimeUnit.SECONDS);

        initClusterMessageConsumer(brokerContext);

    }

    private void initClusterMessageConsumer(BrokerContext brokerContext) {


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

    }

    @Override
    protected void destroyPlugin() {
        enabled = false;
        timer.shutdown();
        //停止核心节点服务
        if (httpServer != null) {
            httpServer.shutdown();
        }

        //中断集群数据监听
        clients.forEach(clientUnit -> {
            if (clientUnit.sseClient != null) {
                clientUnit.sseClient.close();
            }
            if (clientUnit.httpClient != null) {
                clientUnit.httpClient.close();
            }
        });

        try {
            while (queue.poll() != null) ;
            queue.put(SHUTDOWN_MESSAGE);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private void initClusterClient(BrokerContext brokerContext, boolean core) {
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
                    for (ClientUnit clientUnit : clients) {
                        if (clientUnit.httpEnable) {
                            LOGGER.info("send message to cluster");
                            //core节点分发消息至集群其他core节点
                            clientUnit.httpClient.post("/cluster/put/core").header(header -> header.set("access_token", ACCESS_TOKEN).setContentLength(message.getPayload().length).set(ClusterController.HEADER_TOPIC, message.getTopic().getTopic())).body(requestBody -> requestBody.write(message.getPayload())).onFailure(throwable -> {
                                clientUnit.httpEnable = false;
                                LOGGER.error("send message to cluster error", throwable);
                            }).submit();
                        } else {
                            LOGGER.error("send message to cluster error");
                        }
                    }
                } else if (workerClient != null) {
                    workerClient.httpClient.post("/cluster/put/worker").header(header -> header.set("access_token", ACCESS_TOKEN).setContentLength(message.getPayload().length).set(ClusterController.HEADER_TOPIC, message.getTopic().getTopic())).body(requestBody -> requestBody.write(message.getPayload())).onFailure(throwable -> {
                        workerClient.httpEnable = false;
                        LOGGER.error("send message to cluster error", throwable);
                    }).submit();
                }

            }

            @Override
            public boolean enable() {
                return enabled;
            }
        });
    }

    private void initSSE(ClientUnit clientUnit, boolean core) {
        if (clientUnit.sseEnable) {
            return;
        }
        if (clientUnit.sseClient != null) {
            clientUnit.sseClient.close();
            clientUnit.sseClient = null;
        }
        clientUnit.sseEnable = true;
        clientUnit.sseClient = new HttpClient(clientUnit.baseURL);
        clientUnit.sseClient.options().debug(true).group(brokerContext.Options().getChannelGroup());
        //订阅集群推送过来的消息，并投递至总线
        clientUnit.sseClient.post(core ? "/cluster/subscribe/core/" + ACCESS_TOKEN : "/cluster/subscribe/worker/" + ACCESS_TOKEN).onResponseBody(new BinaryServerSentEventStream() {

            @Override
            public void onEvent(HttpResponse httpResponse, String topic, byte[] payload, boolean retained) {
                if (!enabled) {
                    return;
                }
                Message message = new Message(brokerContext.getOrCreateTopic(topic), MqttQoS.AT_MOST_ONCE, payload, retained);
                if (queuePolicy == QUEUE_POLICY_DISCARD_NEWEST) {
                    queue.offer(message);
                } else {
                    while (!queue.offer(message)) {
                        queue.poll();
                    }
                }
            }
        }).onFailure(throwable -> {
            clientUnit.sseEnable = false;
            LOGGER.error("cluster-plugin-sse-client-error", throwable);
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

    public static class ClientUnit {
        private HttpClient sseClient;

        private HttpClient httpClient;

        private boolean httpEnable;
        private boolean sseEnable;

        private boolean checkPending = false;
        private final String baseURL;

        public ClientUnit(String url) {
            this.baseURL = url;
        }
    }
}
