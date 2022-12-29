package org.smartboot.mqtt.broker;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.provider.ConnectAuthenticationProvider;
import org.smartboot.mqtt.common.message.MqttConnectMessage;

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


    private String getHost(MqttSession session) {
        long start = System.currentTimeMillis();
        try {
            return session.getRemoteAddress().getHostName();
        } catch (Exception e) {
            LOGGER.error("get remote address exception", e);
            return "";
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (cost > 1000) {
                LOGGER.warn("InetSocketAddress.getHostName cost: " + cost + "ms");
            }
        }
    }

    @Override
    public boolean authentication(MqttConnectMessage connectMessage, MqttSession session) {
        String username = connectMessage.getPayload().userName();
        String password = connectMessage.getPayload().passwordInBytes() == null ? "" : new String(connectMessage.getPayload().passwordInBytes());
        String configuredUsername = configure.getUsername();
        String configuredPassword = configure.getPassword();
        String host = getHost(session);


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
