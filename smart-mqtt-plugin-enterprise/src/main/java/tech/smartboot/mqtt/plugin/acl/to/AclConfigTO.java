/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.acl.to;


import com.alibaba.fastjson2.annotation.JSONField;

public class AclConfigTO {
    private String type;

    @JSONField(name = "default")
    private AclPasswordConfigTO[] defaultConfigs;

    private AclRestApiConfigTO restapi;

    private long version;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public AclPasswordConfigTO[] getDefaultConfigs() {
        return defaultConfigs;
    }

    public void setDefaultConfigs(AclPasswordConfigTO[] defaultConfigs) {
        this.defaultConfigs = defaultConfigs;
    }

    public AclRestApiConfigTO getRestapi() {
        return restapi;
    }

    public void setRestapi(AclRestApiConfigTO restapi) {
        this.restapi = restapi;
    }
}
