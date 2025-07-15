package tech.smartboot.mqtt.plugin.cluster;

import org.smartboot.socket.timer.HashedWheelTimer;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.mqtt.common.AsyncTask;
import tech.smartboot.mqtt.common.enums.MqttQoS;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.Message;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.bus.MessageBusConsumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

class Coordinator implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(Coordinator.class);
    private static final Message SHUTDOWN_MESSAGE = new Message(null, null, null, false);
    private static final String ACCESS_TOKEN = UUID.randomUUID().toString();
    private static final int QUEUE_POLICY_DISCARD_NEWEST = 0;
    private static final int QUEUE_POLICY_DISCARD_OLDEST = 1;
    private final PluginConfig pluginConfig;
    private final BrokerContext brokerContext;
    private final List<ClusterClient> clients = new ArrayList<>();
    private final HashedWheelTimer timer = new HashedWheelTimer(r -> new Thread(r, "cluster-plugin-health-checker"));
    final VirtualMqttSession mqttSession = new VirtualMqttSession();
    private final Distributor distributor;

    private final Receiver receiver;
    /**
     * 作为worker节点，当配置了多个core节点地址时，只与其中一个节点进行数据同步
     */
    private ClusterClient workerClient;
    private boolean enabled = true;
    private final int queueLength;

    public Coordinator(PluginConfig pluginConfig, BrokerContext brokerContext) {
        this.pluginConfig = pluginConfig;
        this.brokerContext = brokerContext;

        // 队列长度
        int length = pluginConfig.getQueueLength();
        if (length < 1) {
            length = 1024;
        } else if (length > Short.MAX_VALUE) {
            length = Short.MAX_VALUE;
        }
        this.queueLength = length;


        distributor = new Distributor();
        new Thread(distributor, "cluster-plugin-distributer").start();

        receiver = new Receiver();
        new Thread(receiver, "cluster-plugin-receiver").start();
    }

    @Override
    public void run() {
        if (FeatUtils.isNotEmpty(pluginConfig.getClusters())) {
            for (String cluster : pluginConfig.getClusters()) {
                clients.add(new ClusterClient(cluster));
            }
        }
        timer.scheduleWithFixedDelay(new AsyncTask() {
            @Override
            public void execute() {
                clients.forEach(clusterClient -> {
                    if (clusterClient.checkPending) {
                        LOGGER.info("check pending message for {}", clusterClient.baseURL);
                        return;
                    }
                    if (clusterClient.httpEnable) {
                        if (pluginConfig.isCore()) {
                            // core节点需要同集群各core节点进行数据同步
                            receiver.receiveClusterMessage(clusterClient);
                        } else if (workerClient == null) {
                            workerClient = clusterClient;
                            receiver.receiveClusterMessage(clusterClient);
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

    private void offer(Message message, ArrayBlockingQueue<Message> clusterMessageQueue) {
        if (pluginConfig.getQueuePolicy() == QUEUE_POLICY_DISCARD_NEWEST) {
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

    public void destroy() {
        enabled = false;
        timer.shutdown();
        //中断集群数据监听
        clients.forEach(clusterClient -> {
            if (clusterClient.sseClient != null) {
                clusterClient.sseClient.close();
            }
            if (clusterClient.httpClient != null) {
                clusterClient.httpClient.close();
            }
        });

        if (receiver != null) {
            receiver.destroy();
        }

        if (distributor != null) {
            distributor.destroy();
        }
    }

    class Receiver implements Runnable {
        /**
         * 来自集群节点推送过来的消息
         */
        private final ArrayBlockingQueue<Message> receiverQueue = new ArrayBlockingQueue<>(queueLength);

        @Override
        public void run() {
            while (enabled) {
                try {
                    Message message = receiverQueue.take();
                    do {
                        if (SHUTDOWN_MESSAGE == message) {
                            break;
                        }
                        //对于core节点，需要将来自其他core节点推送过来的消息分发给woker节点
                        if (pluginConfig.isCore()) {
                            brokerContext.getEventBus().publish(ClusterPlugin.CLIENT_DIRECT_TO_CORE_BROKER, message);
                        }
                        brokerContext.getMessageBus().publish(mqttSession, message);
                    } while ((message = receiverQueue.poll()) != null);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void receiveClusterMessage(ClusterClient clusterClient) {
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
            clusterClient.sseClient.post(pluginConfig.isCore() ? "/cluster/subscribe/core/" + ACCESS_TOKEN : "/cluster/subscribe/worker/" + ACCESS_TOKEN).onResponseBody(new ClusterMessageStream() {
                @Override
                public void stream(HttpResponse response, byte[] bytes, boolean end) throws IOException {
                    if (end) {
                        clusterClient.sseEnable = false;
                    }
                    super.stream(response, bytes, end);
                }

                @Override
                public void onEvent(HttpResponse httpResponse, String topic, byte[] payload, boolean retained) {
                    if (!Coordinator.this.enabled) {
                        LOGGER.warn("cluster-plugin-consume-message-error");
                        return;
                    }
                    Message message = new Message(brokerContext.getOrCreateTopic(topic), MqttQoS.AT_MOST_ONCE, payload, retained);
                    offer(message, receiverQueue);
                }
            }).onFailure(throwable -> {
                clusterClient.sseEnable = false;
                LOGGER.error("cluster-plugin-sse-client-error", throwable);
            }).onSuccess(httpResponse -> {
                LOGGER.info("cluster-plugin-sse-finish");
                clusterClient.sseEnable = false;
            }).submit();
        }


        public void destroy() {
            try {
                while (receiverQueue.poll() != null) ;
                receiverQueue.put(SHUTDOWN_MESSAGE);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }


    }

    class Distributor implements Runnable {
        /**
         * 当前节点需要分发消息的队列
         */
        private final ArrayBlockingQueue<Message> distributorQueue = new ArrayBlockingQueue<>(queueLength);

        public void run() {
            //将消息总线中的消息发送给集群
            brokerContext.getMessageBus().consumer(new MessageBusConsumer() {
                @Override
                public void consume(MqttSession session, Message message) {
                    //忽略来自集群的消息,包括core和worker节点
                    if (session == mqttSession) {
                        return;
                    }
                    //只有直接连接该core节点的客户端消息才会触发以下逻辑
                    offer(message, distributorQueue);
                }

                @Override
                public boolean enable() {
                    return enabled;
                }
            });
            while (enabled) {
                try {
                    Message nextMessage = distributorQueue.take();
                    do {
                        if (SHUTDOWN_MESSAGE == nextMessage) {
                            break;
                        }
                        //分发给各节点
                        final Message message = nextMessage;
                        if (pluginConfig.isCore()) {
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
                            brokerContext.getEventBus().publish(ClusterPlugin.CLIENT_DIRECT_TO_CORE_BROKER, message);
                        } else if (workerClient != null) {
                            workerClient.httpClient.post("/cluster/put/worker").header(header -> header.keepalive(true).set("access_token", ACCESS_TOKEN).setContentLength(message.getPayload().length).set(ClusterController.HEADER_TOPIC, message.getTopic().getTopic())).body(requestBody -> requestBody.write(message.getPayload())).onFailure(throwable -> {
                                workerClient.httpEnable = false;
                                LOGGER.error("send message to cluster error", throwable);
                            }).submit();
                        }
                    } while ((nextMessage = distributorQueue.poll()) != null);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }

        public void destroy() {
            try {
                while (distributorQueue.poll() != null) ;
                distributorQueue.put(SHUTDOWN_MESSAGE);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class ClusterClient {
        HttpClient sseClient;

        HttpClient httpClient;

        boolean httpEnable;
        boolean sseEnable;

        boolean checkPending = false;
        final String baseURL;

        public ClusterClient(String url) {
            this.baseURL = url;
        }
    }
}