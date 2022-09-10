package org.smartboot.mqtt.broker;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * @author qinluo
 * @date 2022-08-05 16:45:50
 * @since 1.0.0
 */
public class ConfiguredAuthenticationServiceImpl implements AuthenticationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfiguredAuthenticationServiceImpl.class);

    private final BrokerConfigure configure;

    public ConfiguredAuthenticationServiceImpl(BrokerContext context) {
        configure = context.getBrokerConfigure();
    }

    @Override
    public boolean authentication(String username, String password, MqttSession session) {
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
        }  else {
            LOGGER.info("auth failed, ip:{} clientId: {}, username: {}", host, session.getClientId(), username);
        }

        return auth;
    }

    private static String getHost(MqttSession session) {
        try {
            return session.getRemoteAddress().getHostName();
        } catch (Exception e) {
            return "";
        }
    }
}
