package org.smartboot.mqtt.plugin.openapi.to;

import com.alibaba.fastjson2.annotation.JSONField;
import org.smartboot.mqtt.broker.Options;
import org.smartboot.mqtt.common.ToString;

public class VersionTO extends ToString {
    private String name;
    @JSONField(name = "tag_name")
    private String tagName;

    private String body;

    private String current = Options.VERSION;

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getCurrent() {
        return current;
    }

    public void setCurrent(String current) {
        this.current = current;
    }
}
