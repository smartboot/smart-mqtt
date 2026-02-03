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
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.smartboot.socket.timer.HashedWheelTimer;
import tech.smartboot.feat.cloud.RestResult;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.mqtt.common.AsyncTask;
import tech.smartboot.mqtt.common.util.ValidateUtils;
import tech.smartboot.mqtt.plugin.EnterprisePlugin;
import tech.smartboot.mqtt.plugin.convert.SubscriptionConvert;
import tech.smartboot.mqtt.plugin.dao.mapper.SubscriberMapper;
import tech.smartboot.mqtt.plugin.dao.mapper.SystemConfigMapper;
import tech.smartboot.mqtt.plugin.dao.model.SubscriptionDO;
import tech.smartboot.mqtt.plugin.dao.model.TopicStatisticsDO;
import tech.smartboot.mqtt.plugin.dao.query.SubscriberQuery;
import tech.smartboot.mqtt.plugin.openapi.OpenApi;
import tech.smartboot.mqtt.plugin.openapi.enums.RecordTypeEnum;
import tech.smartboot.mqtt.plugin.openapi.enums.SystemConfigEnum;
import tech.smartboot.mqtt.plugin.openapi.to.Pagination;
import tech.smartboot.mqtt.plugin.openapi.to.SubscriptionTO;
import tech.smartboot.mqtt.plugin.openapi.to.TopicStatisticsTO;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;
import tech.smartboot.mqtt.plugin.spec.bus.EventType;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/23
 */
@Controller
public class TopicController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopicController.class);
    @Autowired
    private SubscriberMapper subscriberMapper;

    @Autowired
    private BrokerContext brokerContext;
    @Autowired
    private SqlSessionFactory sessionFactory;

    @Autowired
    private SystemConfigMapper systemConfigMapper;
    private final ConcurrentLinkedQueue<Consumer<SqlSession>> consumers = new ConcurrentLinkedQueue<>();
    private long lastestTime = System.currentTimeMillis();

    @PostConstruct
    public void init() {
        String value = systemConfigMapper.getConfig(SystemConfigEnum.SUBSCRIBE_RECORD.getCode());
        if (!RecordTypeEnum.DB.getCode().equals(value)) {
            LOGGER.debug("subscribe record type:{}", value);
            return;
        }
        brokerContext.getEventBus().subscribe(EventType.SUBSCRIBE_ACCEPT, (eventType, object) -> {
            SubscriptionDO subscriptionDO = new SubscriptionDO();
            subscriptionDO.setClientId(object.getSession().getClientId());
            subscriptionDO.setTopic(object.getObject().getTopicFilter());
            subscriptionDO.setQos(object.getObject().getQualityOfService().value());
            subscriptionDO.setNodeId("smart-mqtt");
            consumers.offer(session -> {
                SubscriberMapper mapper = session.getMapper(SubscriberMapper.class);
                mapper.insert(subscriptionDO);
            });
        });

        EnterprisePlugin.SelfRescueTimer.scheduleWithFixedDelay(new AsyncTask() {
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

        HashedWheelTimer.DEFAULT_TIMER.scheduleWithFixedDelay(new AsyncTask() {
            @Override
            public void execute() {
                lastestTime = System.currentTimeMillis();
                if (consumers.isEmpty()) {
                    LOGGER.debug("batch consume 0 records");
                    return;
                }
                int i = 0;
                try (SqlSession session = sessionFactory.openSession(ExecutorType.BATCH)) {
                    Consumer<SqlSession> consumer;
                    while (i++ < 500 && (consumer = consumers.poll()) != null) {
                        consumer.accept(session);
                    }
                    session.commit(true);
                }
                LOGGER.info("batch consume {} records, cost: {}ms", i, (System.currentTimeMillis() - lastestTime));
            }
        }, 1000, TimeUnit.MILLISECONDS);
    }

    @RequestMapping(OpenApi.SUBSCRIPTIONS_SUBSCRIPTION)
    public RestResult<Pagination<SubscriptionTO>> subscription(SubscriberQuery query) {
        ValidateUtils.notNull(query, "query is null");
        PageHelper.offsetPage((query.getPageNo() - 1) * query.getPageSize(), query.getPageSize());
        Page<SubscriptionDO> list = (Page<SubscriptionDO>) subscriberMapper.select(query);
        Pagination<SubscriptionTO> pagination = new Pagination<>();
        pagination.setPageSize(query.getPageSize());
        pagination.setTotal(list.getTotal());
        pagination.setList(SubscriptionConvert.convert(list));
        return RestResult.ok(pagination);
    }

    @RequestMapping(OpenApi.SUBSCRIPTIONS_TOPICS)
    public RestResult<Pagination<TopicStatisticsTO>> topics(SubscriberQuery query) {
        ValidateUtils.notNull(query, "query is null");
        PageHelper.offsetPage((query.getPageNo() - 1) * query.getPageSize(), query.getPageSize());
        Page<TopicStatisticsDO> list = (Page<TopicStatisticsDO>) subscriberMapper.selectGroupByTopic(query);
        Pagination<TopicStatisticsTO> pagination = new Pagination<>();
        pagination.setPageSize(query.getPageSize());
        pagination.setTotal(list.getTotal());
        pagination.setList(SubscriptionConvert.convertList(list));
        return RestResult.ok(pagination);
    }

    public void setSubscriberMapper(SubscriberMapper subscriberMapper) {
        this.subscriberMapper = subscriberMapper;
    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    public void setSessionFactory(SqlSessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void setSystemConfigMapper(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }
}
