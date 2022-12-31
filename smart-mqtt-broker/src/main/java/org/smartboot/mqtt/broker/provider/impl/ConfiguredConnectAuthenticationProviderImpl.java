package org.smartboot.mqtt.broker.provider.impl;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerConfigure;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.MqttSession;
import org.smartboot.mqtt.broker.provider.ConnectAuthenticationProvider;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.util.MqttUtil;

import java.util.Objects;

/**
 * @author qinluo
 * @date 2022-08-05 16:45:50
 * @since 1.0.0
 */
public class ConfiguredConnectAuthenticationProviderImpl implements ConnectAuthenticationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfiguredConnectAuthenticationProviderImpl.class);

    private final BrokerConfigure configure;

    public ConfiguredConnectAuthenticationProviderImpl(BrokerContext context) {
        configure = context.getBrokerConfigure();
    }


    @Override
    public boolean authentication(MqttConnectMessage connectMessage, MqttSession session) {
        String username = connectMessage.getPayload().userName();
        String password = connectMessage.getPayload().passwordInBytes() == null ? "" : new String(connectMessage.getPayload().passwordInBytes());
        String configuredUsername = configure.getUsername();
        String configuredPassword = configure.getPassword();
        String host = MqttUtil.getRemoteAddress(session);


        if (StringUtils.isEmpty(configuredPassword) || StringUtils.isEmpty(configuredUsername)) {
            LOGGER.debug("no-auth success, ip:{} clientId: {}, username: {}", host, session.getClientId(), username);
            return true;
        }

        boolean auth = Objects.equals(configuredUsername, username) && Objects.equals(configuredPassword, password);
        if (auth) {
            LOGGER.info("auth success, ip:{} clientId: {}, username: {}", host, session.getClientId(), username);
        } else {
            LOGGER.info("auth failed, ip:{} clientId: {}, username: {}", host, session.getClientId(), username);
        }

        return auth;
    }
}
