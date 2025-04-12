package org.smartboot.mqtt.plugin.dao;


import com.alibaba.fastjson2.annotation.JSONField;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/15
 */
class Config {

    /**
     * 数据库类型：h2、mysql
     */
    private String dbType;

    private String url;
    @JSONField(serialize = false)
    private String username;
    @JSONField(serialize = false)
    private String password;

    public String getDbType() {
        return dbType;
    }

    public void setDbType(String dbType) {
        this.dbType = dbType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
