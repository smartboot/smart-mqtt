package org.smartboot.mqtt.plugin.acl.to;

import tech.smartboot.feat.cloud.annotation.JSONField;

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
