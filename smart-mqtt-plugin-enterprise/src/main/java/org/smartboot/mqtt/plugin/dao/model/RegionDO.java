package org.smartboot.mqtt.plugin.dao.model;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 6/22/23
 */
public class RegionDO {
    private String name;
    private int value;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
