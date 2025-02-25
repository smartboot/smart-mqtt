package org.smartboot.mqtt.plugin.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import tech.smartboot.feat.cloud.annotation.Bean;

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

    public static final String CONFIG_JDBC_DB_TYPE = "jdbc.dbType";

    @Bean("sessionFactory")
    public SqlSessionFactory sessionFactory() throws IOException {
        String resource = "mybatis/mybatis-config.xml";
        Properties properties = new Properties();
        properties.setProperty(CONFIG_JDBC_URL, StringUtils.defaultString(System.getProperty(CONFIG_JDBC_URL, System.getenv(CONFIG_JDBC_URL)), ""));
        properties.setProperty(CONFIG_JDBC_USERNAME, StringUtils.defaultString(System.getProperty(CONFIG_JDBC_USERNAME, System.getenv(CONFIG_JDBC_USERNAME)), ""));
        properties.setProperty(CONFIG_JDBC_PASSWORD, StringUtils.defaultString(System.getProperty(CONFIG_JDBC_PASSWORD, System.getenv(CONFIG_JDBC_PASSWORD)), ""));
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(inputStream, StringUtils.defaultString(System.getProperty(CONFIG_JDBC_DB_TYPE, System.getenv(CONFIG_JDBC_DB_TYPE)), "h2_mem"), properties);
        ScriptRunner runner = new ScriptRunner(sessionFactory.openSession().getConnection());
        runner.setLogWriter(null);
        runner.runScript(Resources.getResourceAsReader("mybatis/ddl/schema.sql"));
        return sessionFactory;
    }
}
