package org.smartboot.mqtt.common.enums;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/2/19
 */
public enum MetricTypeEnum {
    /**
     * 基础指标：指表达业务实体原子量化属性的且不可再分的概念集合
     */
    BASIC,
    /**
     * 复合指标：指建立在基础指标之上，通过一定运算规则形成的计算指标集合
     */
    COMPOSITE
}
