/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.dao;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.smartboot.mqtt.common.exception.MqttException;
import tech.smartboot.mqtt.common.util.MqttUtil;
import tech.smartboot.mqtt.plugin.AbstractFeature;
import tech.smartboot.mqtt.plugin.PluginConfig;
import tech.smartboot.mqtt.plugin.spec.BrokerContext;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/14
 */
public class DatabasePlugin extends AbstractFeature {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabasePlugin.class);
    private static final String CONFIG_JSON_PATH = "$['broker']['database']";
    private PluginConfig.DataBaseConfig dataBaseConfig;

    public DatabasePlugin(BrokerContext context, PluginConfig pluginConfig) {
        super(context);
        this.dataBaseConfig = pluginConfig.getDataBase();
    }

    @Override
    public String name() {
        return "database";
    }

    @Override
    public void start() throws Exception {
        MqttUtil.updateConfig(dataBaseConfig, "broker.database");
        LOGGER.debug("database config:{}", JSON.toJSONString(dataBaseConfig));
        try {
            if (StringUtils.isNotBlank(dataBaseConfig.getDbType())) {
                System.setProperty(MybatisSessionFactory.CONFIG_JDBC_DB_TYPE, dataBaseConfig.getDbType());
            }
            if (StringUtils.isNotBlank(dataBaseConfig.getUrl())) {
                System.setProperty(MybatisSessionFactory.CONFIG_JDBC_URL, dataBaseConfig.getUrl());
            }
            if (StringUtils.isNotBlank(dataBaseConfig.getUsername())) {
                System.setProperty(MybatisSessionFactory.CONFIG_JDBC_USERNAME, dataBaseConfig.getUsername());
            }
            if (StringUtils.isNotBlank(dataBaseConfig.getPassword())) {
                System.setProperty(MybatisSessionFactory.CONFIG_JDBC_PASSWORD, dataBaseConfig.getPassword());
            }
//            RestfulBootstrap bootstrap = brokerContext.getBundle("openapi");
//            bootstrap.scan("org.smartboot.mqtt.plugins.database");
//            SessionFactory.initSessionFactory(config.getDbType());
        } catch (Exception e) {
            throw new MqttException("initSessionFactory exception", e);
        }
    }
}
