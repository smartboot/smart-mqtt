package org.smartboot.mqtt.plugin.convert;

import org.smartboot.mqtt.plugin.dao.model.SubscriptionDO;
import org.smartboot.mqtt.plugin.dao.model.TopicStatisticsDO;
import org.smartboot.mqtt.plugin.openapi.to.SubscriptionTO;
import org.smartboot.mqtt.plugin.openapi.to.TopicStatisticsTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/26
 */
public class SubscriptionConvert {

    public static TopicStatisticsTO convert(TopicStatisticsDO topicStatisticsDO) {
        if (topicStatisticsDO == null) {
            return null;
        }
        TopicStatisticsTO topicStatisticsTO = new TopicStatisticsTO();
        topicStatisticsTO.setTopic(topicStatisticsDO.getTopic());
        topicStatisticsTO.setClients(topicStatisticsDO.getClients());
        return topicStatisticsTO;
    }

    public static SubscriptionTO convert(SubscriptionDO subscriptionDO) {
        if (subscriptionDO == null) {
            return null;
        }
        SubscriptionTO subscriptionTO = new SubscriptionTO();
        subscriptionTO.setClientId(subscriptionDO.getClientId());
        subscriptionTO.setTopic(subscriptionDO.getTopic());
        subscriptionTO.setBrokerIpAddress(subscriptionDO.getNodeId());
        subscriptionTO.setQos(subscriptionDO.getQos());
        subscriptionTO.setOptions(subscriptionDO.getOptions());
        return subscriptionTO;
    }

    public static List<SubscriptionTO> convert(List<SubscriptionDO> doList) {
        if (doList == null || doList.isEmpty()) {
            return Collections.emptyList();
        }
        List<SubscriptionTO> list = new ArrayList<>(doList.size());
        for (SubscriptionDO subscriptionDO : doList) {
            list.add(convert(subscriptionDO));
        }
        return list;
    }

    public static List<TopicStatisticsTO> convertList(List<TopicStatisticsDO> doList) {
        if (doList == null || doList.isEmpty()) {
            return Collections.emptyList();
        }
        List<TopicStatisticsTO> list = new ArrayList<>(doList.size());
        for (TopicStatisticsDO subscriptionDO : doList) {
            list.add(convert(subscriptionDO));
        }
        return list;
    }
}
