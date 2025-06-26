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
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;
import tech.smartboot.mqtt.plugin.spec.bus.MessageBusConsumer;

import java.io.IOException;
import java.util.ArrayList;
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
    /**
     * 来自集群节点推送过来的消息
     */
    private ArrayBlockingQueue<Message> clusterMessageQueue;
    private int queuePolicy;

    private HttpServer httpServer;
    private final String clientId = "internal-" + System.nanoTime();
    private final List<ClusterClient> clients = new ArrayList<>();
    private final HashedWheelTimer timer = new HashedWheelTimer(r -> new Thread(r, "cluster-plugin-health-checker"));
    private BrokerContext brokerContext;
    private ClusterClient workerClient;
    private final ClusterMqttSession mqttSession = new ClusterMqttSession(clientId);
    private static final String ACCESS_TOKEN = UUID.randomUUID().toString();
    /**
     * 当前节点需要分发消息的队列
     */
    private ArrayBlockingQueue<Message> pendingDistributionQueue;

    public static final EventType<Message> CLIENT_DIRECT_TO_CORE_BROKER = new EventType<>("client_direct_to_core_broker");

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
        clusterMessageQueue = new ArrayBlockingQueue<>(length);
        pendingDistributionQueue = new ArrayBlockingQueue<>(length);

        new Thread(() -> {
            while (enabled) {
                try {
                    Message message = clusterMessageQueue.take();
                    do {
                        if (SHUTDOWN_MESSAGE == message) {
                            break;
                        }
                        //对于core节点，需要将来自其他core节点推送过来的消息分发给woker节点
                        if (pluginConfig.isCore()) {
                            brokerContext.getEventBus().publish(CLIENT_DIRECT_TO_CORE_BROKER, message);
                        }
                        brokerContext.getMessageBus().publish(mqttSession, message);
                    } while ((message = clusterMessageQueue.poll()) != null);
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
                clients.add(new ClusterClient(cluster));
            }
        }
        distributeToCluster(brokerContext, pluginConfig.isCore());

        //服务健康检测
        timer.scheduleWithFixedDelay(new AsyncTask() {
            @Override
            public void execute() {
                clients.forEach(clusterClient -> {
                    if (clusterClient.checkPending) {
                        LOGGER.info("check pending message for {}", clusterClient.baseURL);
                        return;
                    }
                    if (clusterClient.httpEnable) {
                        if (workerClient == null) {
                            workerClient = clusterClient;
                            consumeClusterMessage(clusterClient, pluginConfig.isCore());
                        } else if (!workerClient.sseEnable) { // 释放workerClient，重新分配
                            workerClient.sseClient.close();
                            workerClient = null;
                        }
                        return;
                    }
                    //release old client
                    if (clusterClient.httpClient != null) {
                        clusterClient.httpClient.close();
                        clusterClient.httpClient = null;
                    }
                    clusterClient.httpClient = new HttpClient(clusterClient.baseURL);
                    clusterClient.httpClient.options().debug(true).connectTimeout(5000).group(brokerContext.Options().getChannelGroup());
                    clusterClient.checkPending = true;
                    clusterClient.httpClient.get("/cluster/status").onSuccess(httpResponse -> {
                        LOGGER.info("check node status success.");
                        clusterClient.httpEnable = true;
                        clusterClient.checkPending = false;

                    }).onFailure(throwable -> {
                        clusterClient.httpEnable = false;
                        clusterClient.checkPending = false;
                        LOGGER.error("check node status error", throwable);
                    }).submit();
                });
            }
        }, 1, TimeUnit.SECONDS);

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
        clients.forEach(clusterClient -> {
            if (clusterClient.sseClient != null) {
                clusterClient.sseClient.close();
            }
            if (clusterClient.httpClient != null) {
                clusterClient.httpClient.close();
            }
        });

        try {
            while (clusterMessageQueue.poll() != null) ;
            clusterMessageQueue.put(SHUTDOWN_MESSAGE);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            while (pendingDistributionQueue.poll() != null) ;
            pendingDistributionQueue.put(SHUTDOWN_MESSAGE);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private void distributeToCluster(BrokerContext brokerContext, boolean core) {
        //将消息总线中的消息发送给集群
        brokerContext.getMessageBus().consumer(new MessageBusConsumer() {
            @Override
            public void consume(MqttSession session, Message message) {
                //忽略来自集群的消息,包括core和worker节点
                if (session.getClientId().equals(clientId)) {
                    return;
                }
                //只有直接连接该core节点的客户端消息才会触发以下逻辑
                if (queuePolicy == QUEUE_POLICY_DISCARD_NEWEST) {
                    boolean suc = pendingDistributionQueue.offer(message);
                    if (!suc) {
                        LOGGER.warn("queue is full, discard message: {}", message);
                    }
                } else {
                    while (!clusterMessageQueue.offer(message)) {
                        Message discard = pendingDistributionQueue.poll();
                        if (discard != null) {
                            LOGGER.warn("queue is full, discard message: {}", discard);
                        }
                    }
                }
            }

            @Override
            public boolean enable() {
                return enabled;
            }
        });
        new Thread(() -> {
            while (enabled) {
                try {
                    Message nextMessage = pendingDistributionQueue.take();
                    do {
                        if (SHUTDOWN_MESSAGE == nextMessage) {
                            break;
                        }
                        //分发给各节点
                        final Message message = nextMessage;
                        if (core) {
                            for (ClusterClient clusterClient : clients) {
                                if (clusterClient.httpEnable) {
                                    LOGGER.info("send message to cluster");
                                    //core节点分发消息至集群其他core节点
                                    clusterClient.httpClient.post("/cluster/put/core").header(header -> header.keepalive(true).set("access_token", ACCESS_TOKEN).setContentLength(message.getPayload().length).set(ClusterController.HEADER_TOPIC, message.getTopic().getTopic())).body(requestBody -> requestBody.write(message.getPayload())).onFailure(throwable -> {
                                        clusterClient.httpEnable = false;
                                        LOGGER.error("send message to cluster error", throwable);
                                    }).onSuccess(httpResponse -> {
                                        LOGGER.info("send message to cluster success");
                                    }).submit();
                                } else {
                                    LOGGER.error("send message to cluster error");
                                }
                            }
                            //当客户端直接将消息发送给core节点，需要分发给相连的worker节点
                            brokerContext.getEventBus().publish(CLIENT_DIRECT_TO_CORE_BROKER, message);
                        } else if (workerClient != null) {
                            workerClient.httpClient.post("/cluster/put/worker").header(header -> header.keepalive(true).set("access_token", ACCESS_TOKEN).setContentLength(message.getPayload().length).set(ClusterController.HEADER_TOPIC, message.getTopic().getTopic())).body(requestBody -> requestBody.write(message.getPayload())).onFailure(throwable -> {
                                workerClient.httpEnable = false;
                                LOGGER.error("send message to cluster error", throwable);
                            }).submit();
                        }
                    } while ((nextMessage = clusterMessageQueue.poll()) != null);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "cluster-plugin-distributer").start();
    }

    private void consumeClusterMessage(ClusterClient clusterClient, boolean core) {
        if (clusterClient.sseEnable) {
            return;
        }
        if (clusterClient.sseClient != null) {
            clusterClient.sseClient.close();
            clusterClient.sseClient = null;
        }
        clusterClient.sseEnable = true;
        clusterClient.sseClient = new HttpClient(clusterClient.baseURL);
        clusterClient.sseClient.options().debug(true).group(brokerContext.Options().getChannelGroup());
        //订阅集群推送过来的消息，并投递至总线
        clusterClient.sseClient.post(core ? "/cluster/subscribe/core/" + ACCESS_TOKEN : "/cluster/subscribe/worker/" + ACCESS_TOKEN).onResponseBody(new BinaryServerSentEventStream() {
            @Override
            public void stream(HttpResponse response, byte[] bytes, boolean end) throws IOException {
                if (end) {
                    clusterClient.sseEnable = false;
                }
                super.stream(response, bytes, end);
            }

            @Override
            public void onEvent(HttpResponse httpResponse, String topic, byte[] payload, boolean retained) {
                if (!enabled) {
                    LOGGER.warn("cluster-plugin-consume-message-error");
                    return;
                }
                Message message = new Message(brokerContext.getOrCreateTopic(topic), MqttQoS.AT_MOST_ONCE, payload, retained);
                if (queuePolicy == QUEUE_POLICY_DISCARD_NEWEST) {
                    boolean suc = clusterMessageQueue.offer(message);
                    if (!suc) {
                        LOGGER.warn("queue is full, discard message: {}", message);
                    }
                } else {
                    while (!clusterMessageQueue.offer(message)) {
                        Message discard = clusterMessageQueue.poll();
                        if (discard != null) {
                            LOGGER.warn("queue is full, discard message: {}", discard);
                        }
                    }
                }
            }
        }).onFailure(throwable -> {
            clusterClient.sseEnable = false;
            LOGGER.error("cluster-plugin-sse-client-error", throwable);
        }).onSuccess(httpResponse -> {
            LOGGER.info("cluster-plugin-sse-finish");
            clusterClient.sseEnable = false;
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
