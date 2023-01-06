package org.smartboot.mqtt.common.message.properties;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/6
 */
public class ReasonProperties {
    /**
     * 原因字符串
     */
    private String reasonString;
    /**
     * 用户属性
     */
    private final List<UserProperty> userProperties = new ArrayList<>();

    public String getReasonString() {
        return reasonString;
    }

    public void setReasonString(String reasonString) {
        this.reasonString = reasonString;
    }

    public List<UserProperty> getUserProperties() {
        return userProperties;
    }
}
