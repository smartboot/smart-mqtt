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

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import tech.smartboot.feat.cloud.annotation.Bean;
import tech.smartboot.mqtt.common.util.MqttUtil;
import tech.smartboot.mqtt.plugin.PluginConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/24
 */
@Bean
public class MybatisSessionFactory {
    public static final String CONFIG_JDBC_URL = "jdbc.url";
    public static final String CONFIG_JDBC_USERNAME = "jdbc.username";
    public static final String CONFIG_JDBC_PASSWORD = "jdbc.password";
    private static final String DB_TYPE_H2 = "h2";
    private static final String DB_TYPE_MYSQL = "mysql";
    private static final String DB_TYPE_H2_MEM = "h2_mem";


    @Bean
    public SqlSessionFactory sessionFactory(PluginConfig pluginConfig, File storage) throws IOException {
        PluginConfig.DataBaseConfig dataBaseConfig = pluginConfig.getDataBase();
        if (dataBaseConfig == null) {
            dataBaseConfig = new PluginConfig.DataBaseConfig();
        }
        if (MqttUtil.isBlank(dataBaseConfig.getDbType())) {
            dataBaseConfig.setDbType(DB_TYPE_H2_MEM);
        }
        if (DB_TYPE_H2.equals(dataBaseConfig.getDbType())) {
            dataBaseConfig.setUrl("jdbc:h2:" + new File(storage, "smart-mqtt").getAbsoluteFile() + ";NON_KEYWORDS=value;mode=mysql;");
        }

        String resource = "mybatis/mybatis-config.xml";
        Properties properties = new Properties();
        properties.setProperty(CONFIG_JDBC_URL, MqttUtil.defaultString(dataBaseConfig.getUrl()));
        properties.setProperty(CONFIG_JDBC_USERNAME, MqttUtil.defaultString(dataBaseConfig.getUsername()));
        properties.setProperty(CONFIG_JDBC_PASSWORD, MqttUtil.defaultString(dataBaseConfig.getPassword()));
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream, MqttUtil.defaultString(dataBaseConfig.getDbType(), "h2_mem"), properties);
        ScriptRunner runner = new ScriptRunner(sessionFactory.openSession().getConnection());
        runner.setLogWriter(null);
        runner.runScript(Resources.getResourceAsReader("mybatis/ddl/schema.sql"));
        return sessionFactory;
    }

}
