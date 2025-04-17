/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.acl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import tech.smartboot.mqtt.common.message.MqttConnectMessage;
import tech.smartboot.mqtt.plugin.acl.to.AclPasswordConfigTO;
import tech.smartboot.mqtt.plugin.openapi.enums.SaltTypeEnum;
import tech.smartboot.mqtt.plugin.spec.MqttSession;
import tech.smartboot.mqtt.plugin.utils.SecureUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DefaultStrategy implements AclStrategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStrategy.class);
    private final Map<String, AclPasswordConfigTO> acls = new HashMap<>();

    public DefaultStrategy(AclPasswordConfigTO[] defaultConfigs) {
        if (defaultConfigs == null) {
            return;
        }
        for (AclPasswordConfigTO config : defaultConfigs) {
            acls.put(config.getUsername(), config);
        }
    }

    @Override
    public void acl(MqttSession session, MqttConnectMessage message) {
        AclPasswordConfigTO config = acls.get(message.getPayload().userName());
        if (config == null) {
            LOGGER.warn("there is no match username, skip current acl check");
            return;
        }
        if ("plain".equals(config.getAlg())) {
            if (Arrays.equals(config.getPassword().getBytes(), message.getPayload().passwordInBytes())) {
                session.setAuthorized(true);
            } else {
                MqttSession.connFailAck(MqttConnectReturnCode.BAD_USERNAME_OR_PASSWORD, session);
            }
            return;
        }

        SaltTypeEnum saltTypeEnum = SaltTypeEnum.getByCode(config.getSaltType());
        String pwd = new String(message.getPayload().passwordInBytes());
        switch (saltTypeEnum) {
            case PREFIX:
                pwd = config.getSalt() + pwd;
                break;
            case SUFFIX:
                pwd = pwd + config.getSalt();
                break;
        }
        if ("md5".equals(config.getAlg()) && !SecureUtil.md5(pwd).equals(config.getPassword())) {
            MqttSession.connFailAck(MqttConnectReturnCode.BAD_USERNAME_OR_PASSWORD, session);
        } else if ("sha256".equals(config.getAlg()) && !SecureUtil.shaEncrypt(pwd).equals(config.getPassword())) {
            MqttSession.connFailAck(MqttConnectReturnCode.BAD_USERNAME_OR_PASSWORD, session);
        } else {
            session.setAuthorized(true);
        }
    }
}
