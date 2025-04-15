package org.smartboot.mqtt.plugin.acl;

import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.license.client.License;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.plugin.acl.to.AclConfigTO;
import org.smartboot.mqtt.plugin.dao.mapper.BrokerNodeMapper;
import org.smartboot.mqtt.plugin.dao.mapper.SystemConfigMapper;
import org.smartboot.mqtt.plugin.dao.model.BrokerNodeDO;
import org.smartboot.mqtt.plugin.openapi.enums.AclTypeEnum;
import org.smartboot.mqtt.plugin.openapi.enums.SystemConfigEnum;
import org.smartboot.mqtt.plugin.spec.BrokerContext;
import org.smartboot.mqtt.plugin.spec.MqttSession;
import org.smartboot.mqtt.plugin.spec.bus.EventType;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.core.client.HttpClient;
import tech.smartboot.feat.core.client.HttpPost;
import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.common.HttpStatus;
import tech.smartboot.feat.core.common.utils.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Bean
public class AclFeature {

    private static final Logger LOGGER = LoggerFactory.getLogger(AclFeature.class);

    @Autowired
    private BrokerContext brokerContext;

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    private AclConfigTO aclConfig;

    private AclStrategy aclStrategy;
    @Autowired
    private BrokerNodeMapper brokerNodeMapper;

    @Autowired
    private License license;

    private Set<String> blackUserNames = Collections.emptySet();

    @PostConstruct
    public void init() {
        loadAclStrategy();
        brokerContext.getTimer().scheduleWithFixedDelay(this::loadAclStrategy, 5, TimeUnit.SECONDS);
        brokerContext.getEventBus().subscribe(EventType.CONNECT, (eventType, object) -> {
            MqttSession session = object.getSession();
            if (session.isAuthorized() || session.isDisconnect()) {
                return;
            }
            //License过期，无法建立新链接
            if (license == null || license.getEntity() == null || license.getEntity().getExpireTime() < System.currentTimeMillis()) {
                LOGGER.error("reject connect because of license has expired.");
                MqttSession.connFailAck(MqttConnectReturnCode.SERVER_UNAVAILABLE_5, session);
                return;
            }
            if (aclConfig == null) {
                LOGGER.warn("there none acl config!");
                return;
            }
            if (blackUserNames.contains(object.getObject().getPayload().userName())) {
                LOGGER.warn("skip current acl check!");
                return;
            }
            aclStrategy.acl(session, object.getObject());
        });
    }

    private void loadAclStrategy() {
        blackUserNames = brokerNodeMapper.selectAll().stream().map(BrokerNodeDO::getNodeId).collect(Collectors.toSet());
        String value = systemConfigMapper.getConfig(SystemConfigEnum.ACL.getCode());
        if (StringUtils.isBlank(value)) {
            aclConfig = null;
            aclStrategy = null;
            return;
        }
        AclConfigTO currentConfig = JSONObject.parseObject(value, AclConfigTO.class);
        if (currentConfig.getVersion() == 0) {
            LOGGER.error("acl config error");
        }
        if (aclConfig != null && aclConfig.getVersion() == currentConfig.getVersion()) {
            return;
        }
        LOGGER.debug("update acl configuration.");
        aclConfig = currentConfig;
        if (AclTypeEnum.DEFAULT.getCode().equals(currentConfig.getType())) {
            aclStrategy = new DefaultStrategy(aclConfig.getDefaultConfigs());
        } else if (AclTypeEnum.NONE.getCode().equals(currentConfig.getType())) {
            aclStrategy = (session, message) -> session.setAuthorized(true);
        } else if (AclTypeEnum.RESTAPI.getCode().equals(currentConfig.getType())) {
            aclStrategy = (session, message) -> {
                HttpClient client = null;
                try {
                    client = new HttpClient(currentConfig.getRestapi().getUrl());
                    client.options().group(brokerContext.Options().getChannelGroup());

                    HttpPost post = client.post();
                    post.header(header -> currentConfig.getRestapi().headers().forEach(header::add));
                    Map<String, String> params = new HashMap<>();
                    if (message.getPayload().userName() != null) {
                        params.put("username", message.getPayload().userName());
                    }
                    if (message.getPayload().passwordInBytes() != null) {
                        params.put("password", new String(message.getPayload().passwordInBytes()));
                    }
                    params.put("clientId", session.getClientId());
                    HttpResponse response = post.body().formUrlencoded(params).submit().get();
                    LOGGER.info("restful acl status:{} ,body:{}", response.statusCode(), response.body());
                    if (response.statusCode() == HttpStatus.OK.value()) {
                        session.setAuthorized(true);
                    } else if (response.statusCode() == HttpStatus.UNAUTHORIZED.value()) {
                        MqttSession.connFailAck(MqttConnectReturnCode.NOT_AUTHORIZED, session);
                    } else {
                        LOGGER.error("unexpected response, status:{} body:{}", response.statusCode(), response.body());
                    }
                } catch (Throwable throwable) {
                    LOGGER.error("exception", throwable);
                    MqttSession.connFailAck(MqttConnectReturnCode.SERVER_UNAVAILABLE_5, session);
                } finally {
                    if (client != null) {
                        client.close();
                    }
                }
            };
        } else {
            LOGGER.error("unSupport now...");
            aclStrategy = null;
        }
    }

    public void setBrokerContext(BrokerContext brokerContext) {
        this.brokerContext = brokerContext;
    }

    public void setSystemConfigMapper(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    public void setBrokerNodeMapper(BrokerNodeMapper brokerNodeMapper) {
        this.brokerNodeMapper = brokerNodeMapper;
    }

    public void setLicense(License license) {
        this.license = license;
    }
}
