package org.smartboot.mqtt.plugin.dao;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.common.exception.MqttException;
import org.smartboot.mqtt.common.util.MqttUtil;
import org.smartboot.mqtt.plugin.AbstractFeature;
import org.smartboot.mqtt.plugin.spec.BrokerContext;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/14
 */
public class DatabasePlugin extends AbstractFeature {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabasePlugin.class);
    private static final String CONFIG_JSON_PATH = "$['broker']['database']";

    public DatabasePlugin(BrokerContext context) {
        super(context);
    }

    @Override
    public String name() {
        return "database";
    }

    @Override
    public void start() throws Exception {
        Config config = context.parseConfig(CONFIG_JSON_PATH, Config.class);
        if (config == null) {
            config = new Config();
            config.setDbType("h2_mem");
            LOGGER.debug("none database config, use default memory model");
        }
        MqttUtil.updateConfig(config, "broker.database");
        LOGGER.debug("database config:{}", JSON.toJSONString(config));
        try {
            if (StringUtils.isNotBlank(config.getDbType())) {
                System.setProperty(MybatisSessionFactory.CONFIG_JDBC_DB_TYPE, config.getDbType());
            }
            if (StringUtils.isNotBlank(config.getUrl())) {
                System.setProperty(MybatisSessionFactory.CONFIG_JDBC_URL, config.getUrl());
            }
            if (StringUtils.isNotBlank(config.getUsername())) {
                System.setProperty(MybatisSessionFactory.CONFIG_JDBC_USERNAME, config.getUsername());
            }
            if (StringUtils.isNotBlank(config.getPassword())) {
                System.setProperty(MybatisSessionFactory.CONFIG_JDBC_PASSWORD, config.getPassword());
            }
//            RestfulBootstrap bootstrap = brokerContext.getBundle("openapi");
//            bootstrap.scan("org.smartboot.mqtt.plugins.database");
//            SessionFactory.initSessionFactory(config.getDbType());
        } catch (Exception e) {
            throw new MqttException("initSessionFactory exception", e);
        }
    }
}
