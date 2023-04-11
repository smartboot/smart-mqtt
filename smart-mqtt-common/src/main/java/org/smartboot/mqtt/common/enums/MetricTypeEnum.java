/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

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
