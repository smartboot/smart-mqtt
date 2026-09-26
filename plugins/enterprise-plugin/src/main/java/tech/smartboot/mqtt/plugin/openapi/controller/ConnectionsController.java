/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi.controller;

import com.alibaba.fastjson2.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.mqtt.common.AsyncTask;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.PluginConfig;
import tech.smartboot.mqtt.plugin.convert.ConnectionConvert;
import tech.smartboot.mqtt.plugin.dao.mapper.ConnectionMapper;
import tech.smartboot.mqtt.plugin.dao.mapper.SubscriberMapper;
import tech.smartboot.mqtt.plugin.dao.model.ConnectionDO;
import tech.smartboot.mqtt.plugin.dao.query.ConnectionQuery;
import tech.smartboot.mqtt.plugin.openapi.OpenApi;
import tech.smartboot.mqtt.plugin.openapi.enums.ConnectionStatusEnum;
import tech.smartboot.mqtt.plugin.openapi.to.ClientStateTO;
import tech.smartboot.mqtt.plugin.openapi.to.ConnectionTO;
import tech.smartboot.mqtt.plugin.openapi.to.Pagination;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.Plugin;
import tech.smartboot.mqtt.plugin.spec.bus.AsyncEventObject;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * 连接管理。CONNECT/DISCONNECT 事件统一进入内存队列 consumers,由定时任务每秒消费一批,消费出口由部署形态决定:
 * <ul>
 *     <li>控制面/单机模式(controlUrl 为空):批量写入数据库,并通过内部接口接收数据面节点上报的客户端状态;</li>
 *     <li>数据面模式(controlUrl 非空):批量上报至控制面,本节点不直写连接表。</li>
 * </ul>
 * 内部接口仅供集群节点间通信使用,请勿将 OpenAPI 端口直接暴露至公网。
 *
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/25
 */
@Controller
public class ConnectionsController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionsController.class);

    /**
     * 单次批量消费(落库或上报)的最大事件数
     */
    private static final int MAX_BATCH_SIZE = 500;

    @Autowired
    private BrokerContext brokerContext;

    @Autowired
    private ConnectionMapper connectionMapper;

    @Autowired
    private SqlSessionFactory sessionFactory;
    @Autowired
    private PluginConfig pluginConfig;
    @Autowired
    private Plugin plugin;

    /**
     * 统一事件队列:数据面模式下为待上报状态,控制面/单机模式下为待落库状态
     */
    private final ConcurrentLinkedQueue<ClientStateTO> consumers = new ConcurrentLinkedQueue<>();
    private long lastestTime = System.currentTimeMillis();

    /**
     * 数据面模式下的控制面 HTTP 客户端,其他模式为 null
     */
    private HttpClient controlPlaneClient;

    @PostConstruct
    public void init() {
        if (!pluginConfig.getDatabase().isConnectRecord()) {
            LOGGER.debug("connect record is disabled");
            return;
        }
        String brokerIp = resolveBrokerIp();
        if (!FeatUtils.isBlank(pluginConfig.getControlUrl())) {
            controlPlaneClient = new HttpClient(pluginConfig.getControlUrl());
        }

        plugin.subscribe(EventType.DISCONNECT, (eventType, object) -> {
            ClientStateTO state = new ClientStateTO();
            state.setClientId(object.getClientId());
            state.setOnline(false);
            consumers.offer(state);
        });
        plugin.subscribe(EventType.CONNECT, AsyncEventObject.syncSubscriber((eventType, object) -> {
            consumers.offer(buildClientState(brokerIp, object));
        }));

        plugin.selfRescueTimer().scheduleWithFixedDelay(new AsyncTask() {
            @Override
            public void execute() {
                if (System.currentTimeMillis() - lastestTime < 20000) {
                    return;
                }
                int i = 0;
                while (consumers.poll() != null) {
                    i++;
                }
                LOGGER.error("discard consume {} records", i);
            }
        }, 10000, TimeUnit.MILLISECONDS);
        plugin.timer().scheduleWithFixedDelay(new AsyncTask() {
            @Override
            public void execute() {
                flush();
            }
        }, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * 批量消费:数据面模式上报至控制面,控制面/单机模式写入数据库
     */
    private void flush() {
        if (consumers.isEmpty()) {
            LOGGER.debug("batch consume 0 records");
            return;
        }
        lastestTime = System.currentTimeMillis();
        List<ClientStateTO> batch = new ArrayList<>();
        ClientStateTO item;
        while (batch.size() < MAX_BATCH_SIZE && (item = consumers.poll()) != null) {
            batch.add(item);
        }
        if (controlPlaneClient != null) {
            report(batch);
        } else {
            save(batch);
        }
        LOGGER.info("batch consume {} records, cost: {}ms", batch.size(), System.currentTimeMillis() - lastestTime);
    }

    /**
     * 数据面模式:批量上报至控制面。失败时直接丢弃,与本地模式积压丢弃策略保持一致
     */
    private void report(List<ClientStateTO> batch) {
        byte[] body = JSON.toJSONBytes(batch);
        try {
            tech.smartboot.feat.core.client.HttpResponse response = controlPlaneClient.post(OpenApi.INTERNAL_CLIENTS_REPORT)
                    .header(header -> {
                        header.setContentType("application/json");
                        header.setContentLength(body.length);
                    })
                    .body(requestBody -> requestBody.write(body))
                    .submit().get(5, TimeUnit.SECONDS);
            if (response.statusCode() != HttpStatus.ACCEPTED.value()) {
                LOGGER.warn("report to control plane failed, status: {}", response.statusCode());
            }
        } catch (Throwable throwable) {
            LOGGER.error("report to control plane exception: {}", throwable.getMessage());
        }
    }

    /**
     * 控制面/单机模式:批量落库
     */
    private void save(List<ClientStateTO> batch) {
        try (SqlSession session = sessionFactory.openSession(ExecutorType.BATCH)) {
            SubscriberMapper subscriberMapper = session.getMapper(SubscriberMapper.class);
            ConnectionMapper mapper = session.getMapper(ConnectionMapper.class);
            for (ClientStateTO state : batch) {
                if (state.isOnline()) {
                    subscriberMapper.deleteById(state.getClientId());
                    mapper.deleteById(state.getClientId());
                    mapper.insert(convert(state));
                } else {
                    mapper.updateStatus(state.getClientId(), ConnectionStatusEnum.DIS_CONNECT.getStatus());
                }
            }
            session.commit(true);
        } catch (Throwable throwable) {
            LOGGER.error("batch save exception", throwable);
        }
    }

    private static ConnectionDO convert(ClientStateTO state) {
        ConnectionDO connectionDO = new ConnectionDO();
        connectionDO.setClientId(state.getClientId());
        connectionDO.setUsername(state.getUsername());
        connectionDO.setBrokerIp(state.getBrokerIp());
        connectionDO.setIpAddress(state.getIpAddress());
        connectionDO.setKeepalive(state.getKeepalive());
        connectionDO.setStatus(ConnectionStatusEnum.CONNECTED.getStatus());
        connectionDO.setConnectTime(new Date(state.getConnectTime()));
        return connectionDO;
    }

    /**
     * 解析Broker本机IP
     */
    private static String resolveBrokerIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Throwable e) {
            LOGGER.error("decode broker ip exception", e);
            return "-";
        }
    }

    private static ClientStateTO buildClientState(String brokerIp, AsyncEventObject<MqttConnectMessage> object) {
        ClientStateTO state = new ClientStateTO();
        state.setClientId(object.getSession().getClientId());
        state.setUsername(object.getObject().getPayload().userName());
        state.setBrokerIp(brokerIp);
        try {
            state.setIpAddress(object.getSession().getRemoteAddress().getHostString());
        } catch (Throwable e) {
            state.setIpAddress("-");
            LOGGER.error("decode ip exception", e);
        }
        state.setMqttVersion(object.getSession().getMqttVersion().name());
        state.setOnline(!object.getSession().isDisconnect());
        state.setCleanStart(object.getObject().getVariableHeader().isCleanSession());
        state.setKeepalive(object.getObject().getVariableHeader().keepAliveTimeSeconds());
        state.setConnectTime(System.currentTimeMillis());
        state.setActiveTime(object.getSession().getLatestReceiveMessageTime());
        return state;
    }

    /**
     * 内部接口:数据面客户端状态上报入口。状态入队后立即返回 202,由定时任务批量落库
     */
    @RequestMapping(OpenApi.INTERNAL_CLIENTS_REPORT)
    public RestResult<Void> report(HttpRequest request, HttpResponse response) throws Exception {
        if (controlPlaneClient != null) {
            response.setHttpStatus(HttpStatus.BAD_REQUEST);
            return RestResult.fail("this node is not a control plane node");
        }
        byte[] body = readBody(request);
        List<ClientStateTO> states = JSON.parseArray(new String(body, StandardCharsets.UTF_8), ClientStateTO.class);
        if (states != null && !states.isEmpty()) {
            consumers.addAll(states);
        }
        response.setHttpStatus(HttpStatus.ACCEPTED);
        return RestResult.ok(null);
    }

    private static byte[] readBody(HttpRequest request) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = request.getInputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        return out.toByteArray();
    }

    @RequestMapping(OpenApi.CONNECTIONS)
    public RestResult<Pagination<ConnectionTO>> connections(ConnectionQuery query) {
        ValidateUtils.notNull(query, "query is null");
        PageHelper.offsetPage((query.getPageNo() - 1) * query.getPageSize(), query.getPageSize());
        Page<ConnectionDO> list = (Page<ConnectionDO>) connectionMapper.select(query);
        Pagination<ConnectionTO> pagination = new Pagination<>();
        pagination.setList(ConnectionConvert.convert(list));
        pagination.setTotal(list.getTotal());
        pagination.setPageSize(list.getPageSize());
        return RestResult.ok(pagination);
    }

    @RequestMapping(OpenApi.DIS_CONNECTION)
    public RestResult<Void> disconnection(@Param("clientId") String clientId) {
        MqttSession session = brokerContext.getSession(clientId);
        if (session == null) {
            return RestResult.fail("连接不存在");
        }
        session.disconnect();
        return RestResult.ok(null);
    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    public void setConnectionMapper(ConnectionMapper connectionMapper) {
        this.connectionMapper = connectionMapper;
    }

    public void setSessionFactory(SqlSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void setPluginConfig(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }
}
