/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.to;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
public class MetricTO {
    /**
     * 指标项
     */
    private final Map<String, MetricItemTO> metric = new HashMap<>();

    /**
     * 指标分组
     */
    private final Map<String, List<MetricItemTO>> group = new HashMap<>();


    public Map<String, List<MetricItemTO>> getGroup() {
        return group;
    }

    public Map<String, MetricItemTO> getMetric() {
        return metric;
    }
}
