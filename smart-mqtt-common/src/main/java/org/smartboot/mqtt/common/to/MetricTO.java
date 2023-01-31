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
