/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.broker.topic;

import org.junit.Assert;
import org.junit.Test;
import tech.smartboot.mqtt.common.TopicToken;
import tech.smartboot.mqtt.common.util.MqttUtil;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/3
 */
public class TopicMatcherTest {

    @Test
    public void testMatcher() {
        BrokerTopicImpl topic = new BrokerTopicImpl("/a");
        TopicToken topicToken = new TopicToken("#");
        Assert.assertTrue(MqttUtil.match(topic, topicToken));

        topic = new BrokerTopicImpl("/a");
        topicToken = new TopicToken("/#");
        Assert.assertTrue(MqttUtil.match(topic, topicToken));

        topic = new BrokerTopicImpl("/a");
        topicToken = new TopicToken("+/#");
        Assert.assertTrue(MqttUtil.match(topic, topicToken));

        topic = new BrokerTopicImpl("/a/b/c");
        topicToken = new TopicToken("/#");
        Assert.assertTrue(MqttUtil.match(topic, topicToken));

        topic = new BrokerTopicImpl("/a/b/c");
        topicToken = new TopicToken("#");
        Assert.assertTrue(MqttUtil.match(topic, topicToken));

        topic = new BrokerTopicImpl("/a/b/c");
        topicToken = new TopicToken("/a/b/c");
        Assert.assertTrue(MqttUtil.match(topic, topicToken));

        topic = new BrokerTopicImpl("/a/b/c");
        topicToken = new TopicToken("a/b/c");
        Assert.assertFalse(MqttUtil.match(topic, topicToken));

        topic = new BrokerTopicImpl("/a/b/c");
        topicToken = new TopicToken("/+/b/+");
        Assert.assertTrue(MqttUtil.match(topic, topicToken));

        topic = new BrokerTopicImpl("/a");
        topicToken = new TopicToken("/+");
        Assert.assertTrue(MqttUtil.match(topic, topicToken));

        topic = new BrokerTopicImpl("/a");
        topicToken = new TopicToken("+/+");
        Assert.assertTrue(MqttUtil.match(topic, topicToken));
    }

}
