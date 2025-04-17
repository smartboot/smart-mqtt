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

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.timer.HashedWheelTimer;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.mqtt.common.AsyncTask;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.convert.ConnectionConvert;
import tech.smartboot.mqtt.plugin.dao.mapper.ConnectionMapper;
import tech.smartboot.mqtt.plugin.dao.mapper.SubscriberMapper;
import tech.smartboot.mqtt.plugin.dao.mapper.SystemConfigMapper;
import tech.smartboot.mqtt.plugin.dao.model.ConnectionDO;
import tech.smartboot.mqtt.plugin.dao.query.ConnectionQuery;
import tech.smartboot.mqtt.plugin.openapi.OpenApi;
import tech.smartboot.mqtt.plugin.openapi.enums.ConnectionStatusEnum;
import tech.smartboot.mqtt.plugin.openapi.enums.RecordTypeEnum;
import tech.smartboot.mqtt.plugin.openapi.enums.SystemConfigEnum;
import tech.smartboot.mqtt.plugin.openapi.to.ConnectionTO;
import tech.smartboot.mqtt.plugin.openapi.to.Pagination;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;
import tech.smartboot.mqtt.plugin.utils.IpUtil;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/25
 */
@Controller(async = true)
public class ConnectionsController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionsController.class);
    @Autowired
    private BrokerContext brokerContext;

    @Autowired
    private ConnectionMapper connectionMapper;

    @Autowired
    private SqlSessionFactory sessionFactory;
    @Autowired
    private SystemConfigMapper systemConfigMapper;
    private final ConcurrentLinkedQueue<Consumer<SqlSession>> consumers = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init() {
        String value = systemConfigMapper.getConfig(SystemConfigEnum.CONNECT_RECORD.getCode());
        if (!RecordTypeEnum.DB.getCode().equals(value)) {
            LOGGER.debug("connect record type:{}", value);
            return;
        }
        //可能因Broker异常退出导致原连接状态依旧处于Connected状态，启动时统一订正
//        brokerContext.getEventBus().subscribe(EventType.BROKER_STARTED, (eventType, object) -> {
        connectionMapper.updateStatusByBroker(brokerContext.Options().getNodeId(), ConnectionStatusEnum.DIS_CONNECT.getStatus());
//        });

        brokerContext.getEventBus().subscribe(EventType.DISCONNECT, (eventType, object) -> {
            int r = connectionMapper.updateStatus(object.getClientId(), ConnectionStatusEnum.DIS_CONNECT.getStatus());
        });
        brokerContext.getEventBus().subscribe(EventType.CONNECT, (eventType, object) -> {
            consumers.offer(session -> {
                SubscriberMapper subscriberMapper = session.getMapper(SubscriberMapper.class);
                subscriberMapper.deleteById(object.getSession().getClientId());
                MqttConnectMessage message = object.getObject();
                ConnectionMapper mapper = session.getMapper(ConnectionMapper.class);
                ConnectionDO connectionDO = new ConnectionDO();
                connectionDO.setClientId(object.getSession().getClientId());
                connectionDO.setUsername(message.getPayload().userName());
                connectionDO.setNodeId(brokerContext.Options().getNodeId());
                try {
                    connectionDO.setIpAddress(object.getSession().getRemoteAddress().getHostString());
                    String region = IpUtil.search(connectionDO.getIpAddress());
                    String[] array = StringUtils.split(region, "|");
                    if (array.length == 5) {
                        connectionDO.setCountry(array[0]);
                        connectionDO.setRegion(array[1]);
                        connectionDO.setProvince(array[2]);
                        connectionDO.setCity(array[3]);
                        connectionDO.setIsp(array[4]);
                    } else {
                        LOGGER.error("unexpected ip:{} region: {}", connectionDO.getIpAddress(), region);
                    }
                } catch (IOException e) {
                    connectionDO.setIpAddress("-");
                    LOGGER.error(e.getMessage());
                }
                connectionDO.setStatus(object.getSession().isDisconnect() ? ConnectionStatusEnum.DIS_CONNECT.getStatus() : ConnectionStatusEnum.CONNECTED.getStatus());
                connectionDO.setKeepalive(message.getVariableHeader().keepAliveTimeSeconds());
                connectionDO.setConnectTime(new Date());
                mapper.deleteById(connectionDO.getClientId());
                int r = mapper.insert(connectionDO);
//                int i = 1000;
//                while (i-- > 0) {
//                    connectionDO.setClientId(UUID.randomUUID().toString().substring(0, 32));
//                    connectionDO.setIpAddress(((int) (Math.random() * 255) + 1) + "." + ((int) (Math.random() * 255) + 1) + "." + ((int) (Math.random() * 255) + 1) + "." + ((int) (Math.random() *
//                    255) + 1));
//                    String region = IpUtil.search(connectionDO.getIpAddress());
//                    String[] array = StringUtils.split(region, "|");
//                    if (array.length == 5) {
//                        connectionDO.setCountry(array[0]);
//                        connectionDO.setRegion(array[1]);
//                        connectionDO.setProvince(array[2]);
//                        connectionDO.setCity(array[3]);
//                        connectionDO.setIsp(array[4]);
//                    } else {
//                        LOGGER.error("unexpected ip:{} region: {}", connectionDO.getIpAddress(), region);
//                    }
//                    mapper.insert(connectionDO);
//                }
//                LOGGER.info("insert connection:{}", JSON.toJSONString(connectionDO));
            });
        });
        HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(new AsyncTask() {
            @Override
            public void execute() {
                if (consumers.isEmpty()) {
                    return;
                }
                int i = 0;
                try (SqlSession session = sessionFactory.openSession(ExecutorType.BATCH, true)) {
                    Consumer<SqlSession> consumer;
                    while (i++ < 100 && (consumer = consumers.poll()) != null) {
                        try {
                            consumer.accept(session);
                        } catch (Throwable throwable) {
                            LOGGER.error("batch consume  exception", throwable);
                        }
                    }
                    session.commit();
                }
            }
        }, 1000, TimeUnit.MILLISECONDS);
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

    public void setSystemConfigMapper(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }
}
