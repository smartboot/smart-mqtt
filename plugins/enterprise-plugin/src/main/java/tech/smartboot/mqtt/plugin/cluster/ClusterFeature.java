/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.cluster;

import com.alibaba.fastjson2.JSONObject;
import com.sun.management.OperatingSystemMXBean;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.mqtt.client.MqttClient;
import tech.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import tech.smartboot.mqtt.common.message.payload.MqttConnectPayload;
import tech.smartboot.mqtt.common.util.MqttUtil;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.PluginConfig;
import tech.smartboot.mqtt.plugin.dao.mapper.BrokerNodeMapper;
import tech.smartboot.mqtt.plugin.dao.model.BrokerNodeDO;
import tech.smartboot.mqtt.plugin.openapi.enums.BrokerNodeTypeEnum;
import tech.smartboot.mqtt.plugin.openapi.enums.BrokerStatueEnum;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.Options;
import tech.smartboot.mqtt.plugin.spec.bus.EventBus;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;
import tech.smartboot.mqtt.plugin.utils.SecureUtil;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 6/6/23
 */
public class ClusterFeature {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterFeature.class);


    /**
     * 集群内其他协调节点建立的连接
     */
    private final Map<MqttSession, String> coreSessions = new ConcurrentHashMap<>();

    private final Map<MqttSession, String> workerSessions = new ConcurrentHashMap<>();

    /**
     * 与当前节点直连的节点
     */
    private final Map<String, MqttClient> connectedNodes = new ConcurrentHashMap<>();

    @Autowired
    private BrokerNodeMapper brokerNodeMapper;


    @Autowired
    private BrokerContext brokerContext;

    @Autowired
    private PluginConfig pluginConfig;


    @PostConstruct
    public void init() throws Exception {
        if (MqttUtil.isBlank(pluginConfig.getNodeType())) {
            pluginConfig.setNodeType(BrokerNodeTypeEnum.CORE_NODE.getCode());
            LOGGER.error("broker.nodeType is blank, set default config: core");
        }
        if (pluginConfig.getClusterEndpoint() == null) {
            LOGGER.error("broker.clusterEndpoint is null, cluster disabled");
            pluginConfig.setClusterEndpoint("");
        }
//        int limit = NumberUtils.toInt(properties.getProperty("cluster.limit"), 1);
        ValidateUtils.notNull(brokerContext.Options().getNodeId(), "broker.nodeId is null");
        BrokerNodeDO nodeDO = brokerNodeMapper.selectById(brokerContext.Options().getNodeId());
        if (nodeDO == null) {
            nodeDO = new BrokerNodeDO();
            nodeDO.setNodeId(brokerContext.Options().getNodeId());
            nodeDO.setNodeType(pluginConfig.getNodeType());

            setNodeDO(nodeDO, pluginConfig);

            brokerNodeMapper.insert(nodeDO);
        } else {
            ValidateUtils.isTrue(FeatUtils.equals(nodeDO.getNodeType(), pluginConfig.getNodeType()), "nodeType is different from before.");
            if (FeatUtils.equals(nodeDO.getStatus(), BrokerStatueEnum.RUNNING.getCode())) {
                LOGGER.warn("This node did not exit normally previously.");
            }

            setNodeDO(nodeDO, pluginConfig);

            brokerNodeMapper.update(nodeDO);
        }
        int count = brokerNodeMapper.count();
//        ValidateUtils.isTrue(count <= limit, "");
        if (FeatUtils.equals(BrokerNodeTypeEnum.WORKER_NODE.getCode(), nodeDO.getNodeType())) {
            initWorkerNode(nodeDO);
        } else {
            initCoreNode(nodeDO);
        }
        brokerContext.getTimer().schedule(new Runnable() {
            @Override
            public void run() {
                BrokerNodeDO node = new BrokerNodeDO();
                node.setNodeId(brokerContext.Options().getNodeId());
                node.setProcess(JSONObject.toJSONString(ClusterFeature.this.getCurrentNode()));
                brokerNodeMapper.update(node);
                brokerContext.getTimer().schedule(this, 5, TimeUnit.SECONDS);
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void setNodeDO(BrokerNodeDO nodeDO, PluginConfig config) {
        nodeDO.setCoreNodeId(config.getCoreNodeId());
        nodeDO.setClusterEndpoint(config.getClusterEndpoint());
        nodeDO.setIpAddress(brokerContext.Options().getHost());
        nodeDO.setStatus(BrokerStatueEnum.RUNNING.getCode());
        nodeDO.setPort(brokerContext.Options().getPort());
        nodeDO.setStartTime(new Date());
        if (FeatUtils.isBlank(nodeDO.getClusterEndpoint())) {
            nodeDO.setStatus(BrokerStatueEnum.UNHEALTHY.getCode());
        }
    }

    private void initWorkerNode(BrokerNodeDO currentNode) {
        subscribeWorkerEventBus(currentNode);
        brokerContext.getTimer().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    BrokerNodeDO coordinationNode = brokerNodeMapper.selectById(currentNode.getCoreNodeId());
                    if (coordinationNode == null || !BrokerStatueEnum.RUNNING.getCode().equals(coordinationNode.getStatus())) {
                        LOGGER.warn("coordination node is not ready!");
                        connectedNodes.values().forEach(MqttClient::disconnect);
                        connectedNodes.clear();
                        return;
                    }
                    //新加入的协调节点
                    if (!connectedNodes.containsKey(coordinationNode.getNodeId())) {
                        connectBroker(currentNode, coordinationNode);
                    }
                } finally {
                    brokerContext.getTimer().schedule(this, 5, TimeUnit.SECONDS);
                }
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void initCoreNode(BrokerNodeDO currentNode) {
        subscribeCoordinationEventBus(currentNode);
        //定期更新集群
        brokerContext.getTimer().schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    List<BrokerNodeDO> clusterNodes = brokerNodeMapper.selectCoordinationConnections(currentNode.getNodeId()).stream().filter(nodeDO -> !nodeDO.getNodeId().equals(currentNode.getNodeId())).collect(Collectors.toList());
                    List<String> nodeIds = clusterNodes.stream().map(BrokerNodeDO::getNodeId).collect(Collectors.toList());
                    //移除不存在的集群节点
                    connectedNodes.keySet().stream().filter(nodeId -> !nodeIds.contains(nodeId)).forEach(nodeId -> connectedNodes.remove(nodeId).disconnect());

                    clusterNodes.forEach(node -> {
                        //移除已离线的连接
                        if (!BrokerStatueEnum.RUNNING.getCode().equals(node.getStatus())) {
                            MqttClient client = connectedNodes.remove(node.getNodeId());
                            if (client != null) {
                                client.disconnect();
                            }
                            return;
                        }
                        //新加入的协调节点
                        if (!connectedNodes.containsKey(node.getNodeId())) {
                            connectBroker(currentNode, node);
                        }
                    });
                } finally {
                    brokerContext.getTimer().schedule(this, 5, TimeUnit.SECONDS);
                }
            }
        }, 5, TimeUnit.SECONDS);
    }

    private void connectBroker(BrokerNodeDO currentNode, BrokerNodeDO clusterNode) {
        ValidateUtils.notBlank(clusterNode.getClusterEndpoint(), "clusterEndpoint is blank");
        ValidateUtils.isTrue(!FeatUtils.equals(currentNode.getNodeId(), clusterNode.getNodeId()), "invalid cluster config");
        MqttClient mqttClient = new MqttClient(clusterNode.getClusterEndpoint(), options -> {
            options.setGroup(brokerContext.Options().getChannelGroup())
                    .setAutomaticReconnect(true)
                    .setKeepAliveInterval(30)
                    .setUserName(currentNode.getNodeId())
                    .setPassword(getPassword(currentNode, clusterNode));
        });
        connectedNodes.put(clusterNode.getNodeId(), mqttClient);
        mqttClient.connect(mqttConnAckMessage -> {
            LOGGER.info("Successfully connected to cluster {} node: {}", clusterNode.getNodeType(), clusterNode.getNodeId());
        });
    }

    private byte[] getPassword(BrokerNodeDO currentNode, BrokerNodeDO coordinationNode) {
        return SecureUtil.shaEncrypt(currentNode.getNodeId() + ":" + coordinationNode.getNodeId()).getBytes();
    }

    private void subscribeWorkerEventBus(BrokerNodeDO currentNode) {
        EventBus eventBus = brokerContext.getEventBus();
        eventBus.subscribe(EventType.CONNECT, (eventType, object) -> {
            MqttSession session = object.getSession();
            if (session.isAuthorized() || session.isDisconnect()) {
                return;
            }
            //该连接是集群内其他Broker建立的
            MqttConnectPayload payload = object.getObject().getPayload();
            BrokerNodeDO nodeDO = brokerNodeMapper.selectById(payload.userName());
            if (nodeDO == null) {
                LOGGER.warn("connect by external mqttClient");
                return;
            }
            //来源非core节点
            if (!FeatUtils.equals(BrokerNodeTypeEnum.CORE_NODE.getCode(), nodeDO.getNodeType())) {
                LOGGER.error("invalid node connection!");
                MqttSession.connFailAck(MqttConnectReturnCode.IMPLEMENTATION_SPECIFIC_ERROR, session);
                return;
            }
            //工作节点
            if (currentNode.getNodeId().equals(payload.userName())) {
                LOGGER.error("invalid connection");
                MqttSession.connFailAck(MqttConnectReturnCode.BANNED, session);
                return;
            }
//            System.out.println("aa:" + new String(payload.passwordInBytes()));
//            System.out.println("bb:" + new String(getPassword(nodeDO, currentNode)));
//            System.out.println("cc:" + new String(getPassword(currentNode, nodeDO)));
            if (!Arrays.equals(payload.passwordInBytes(), getPassword(nodeDO, currentNode))) {
                LOGGER.error("invalid password");
                MqttSession.connFailAck(MqttConnectReturnCode.BAD_USERNAME_OR_PASSWORD, session);
                return;
            }
            coreSessions.put(object.getSession(), nodeDO.getNodeId());
            session.setAuthorized(true);
        });
        //集群连接断开，清理缓存
        eventBus.subscribe(EventType.DISCONNECT, (eventType, object) -> {
            coreSessions.remove(object);
        });
        //集群消息分发
        brokerContext.getMessageBus().consumer((mqttSession, message) -> {
            //协调节点推送的消息不进行分发
            if (coreSessions.containsKey(mqttSession)) {
                return;
            }
            //转发给协调节点或工作节点
            ValidateUtils.isTrue(connectedNodes.size() <= 1, "invalid connections");
            connectedNodes.forEach((nodeId, session) -> {
                LOGGER.info("dispatch message to coreNode:{}", nodeId);
                session.publish(message.getTopic().getTopic(), message.getQos(), message.getPayload(), message.isRetained(), new Consumer<Integer>() {
                    @Override
                    public void accept(Integer integer) {
//                        System.out.println("messageId:" + integer);
                    }
                });
            });
        });
    }

    private void subscribeCoordinationEventBus(BrokerNodeDO currentNode) {
        EventBus eventBus = brokerContext.getEventBus();
        eventBus.subscribe(EventType.CONNECT, (eventType, object) -> {
            MqttSession session = object.getSession();
            if (session.isAuthorized() || session.isDisconnect()) {
                return;
            }
            //该连接是集群内其他Broker建立的
            MqttConnectPayload payload = object.getObject().getPayload();
            BrokerNodeDO nodeDO = brokerNodeMapper.selectById(payload.userName());
            if (nodeDO == null) {
                LOGGER.warn("connect by external mqttClient");
                return;
            }
//            System.out.println("bb mqttClient:" + object.getSession().getClientId());
            if (currentNode.getNodeId().equals(payload.userName())) {
                LOGGER.error("invalid connection, expect:{} actual:{}", currentNode.getNodeId(), payload.userName());
                MqttSession.connFailAck(MqttConnectReturnCode.BAD_USERNAME_OR_PASSWORD, session);
                return;
            }
            if (!Arrays.equals(payload.passwordInBytes(), getPassword(nodeDO, currentNode))) {
                LOGGER.error("invalid password");
                MqttSession.connFailAck(MqttConnectReturnCode.BAD_USERNAME_OR_PASSWORD, session);
                return;
            }

            //工作节点
            if (FeatUtils.equals(BrokerNodeTypeEnum.WORKER_NODE.getCode(), nodeDO.getNodeType())) {
                ValidateUtils.isTrue(FeatUtils.equals(currentNode.getNodeId(), nodeDO.getCoreNodeId()), "invalid worker node connection!");
                LOGGER.info("{} node: {} connect success!", nodeDO.getNodeType(), nodeDO.getNodeId());
                workerSessions.put(object.getSession(), nodeDO.getNodeId());
            } else {
                coreSessions.put(object.getSession(), nodeDO.getNodeId());
            }
            session.setAuthorized(true);
        });
        //集群连接断开，清理缓存
        eventBus.subscribe(EventType.DISCONNECT, (eventType, object) -> {
            coreSessions.remove(object);
            workerSessions.remove(object);
        });
        //集群消息分发
        brokerContext.getMessageBus().consumer((mqttSession, message) -> {
            //转发给协调节点或工作节点
            if (coreSessions.containsKey(mqttSession)) {
                LOGGER.info("receive message from coreNode:{}", coreSessions.get(mqttSession));
                workerSessions.values().forEach(nodeId -> {
                    LOGGER.info("分发消息至Worker节点:{}", nodeId);
                    connectedNodes.get(nodeId).publish(message.getTopic().getTopic(), message.getQos(), message.getPayload(), message.isRetained(), new Consumer<Integer>() {
                        @Override
                        public void accept(Integer integer) {
//                            System.out.println("messageId:" + integer);
                        }
                    });
                });
            } else {
                String workerNodeId = workerSessions.get(mqttSession);
                connectedNodes.forEach((nodeId, session) -> {
                    if (FeatUtils.equals(nodeId, workerNodeId)) {
                        LOGGER.info("ignore dispatch...");
                        return;
                    }
                    LOGGER.info("分发消息至集群节点:{}", nodeId);
                    connectedNodes.get(nodeId).publish(message.getTopic().getTopic(), message.getQos(), message.getPayload(), message.isRetained(), new Consumer<Integer>() {
                        @Override
                        public void accept(Integer integer) {
//                            System.out.println("messageId:" + integer);
                        }
                    });
                });
            }
        });

    }


    @PreDestroy
    public void destroy() {
        if (brokerContext == null) {
            return;
        }
        LOGGER.info("destroy node: {}...", brokerContext.Options().getNodeId());
        BrokerNodeDO node = new BrokerNodeDO();
        node.setNodeId(brokerContext.Options().getNodeId());
        node.setStatus(BrokerStatueEnum.STOPPED.getCode());
        brokerNodeMapper.update(node);
        connectedNodes.values().forEach(MqttClient::disconnect);
    }

    private NodeProcessInfo getCurrentNode() {
        NodeProcessInfo info = new NodeProcessInfo();
        info.setVersion(Options.VERSION);
        info.setVmVendor(System.getProperty("java.vendor"));
        info.setVmVersion(System.getProperty("java.version"));
        info.setOsName(System.getProperty("os.name"));
        info.setOsArch(System.getProperty("os.arch"));
        info.setOsVersion(System.getProperty("os.name") + " " + System.getProperty("os.version"));
        info.setHostName(System.getProperty("user.name"));

        OperatingSystemMXBean systemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        info.setCpuUsage((int) (systemMXBean.getSystemCpuLoad() * 100));
        // 获取运行时对象
        Runtime runtime = Runtime.getRuntime();

        // 获取总内存（以字节为单位）
        long totalMemory = runtime.totalMemory();
        // 计算内存使用率（以百分比表示）
        info.setMemoryLimit(totalMemory);
        info.setMemUsage(totalMemory - runtime.freeMemory());
        return info;
    }

    public void setBrokerNodeMapper(BrokerNodeMapper brokerNodeMapper) {
        this.brokerNodeMapper = brokerNodeMapper;
    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    public void setPluginConfig(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }
}
