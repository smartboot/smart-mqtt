package org.smartboot.mqtt.plugin.acl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.MqttSession;
import org.smartboot.mqtt.common.enums.MqttConnectReturnCode;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.plugin.acl.to.AclPasswordConfigTO;
import org.smartboot.mqtt.plugin.openapi.enums.SaltTypeEnum;
import org.smartboot.mqtt.plugin.utils.SecureUtil;

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
