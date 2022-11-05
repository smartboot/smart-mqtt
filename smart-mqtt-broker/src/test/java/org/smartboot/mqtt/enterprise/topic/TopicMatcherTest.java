package org.smartboot.mqtt.enterprise.topic;

import org.junit.Assert;
import org.junit.Test;
import org.smartboot.mqtt.common.Topic;
import org.smartboot.mqtt.common.TopicToken;
import org.smartboot.mqtt.common.util.TopicTokenUtil;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/3
 */
public class TopicMatcherTest {

    @Test
    public void testMatcher() {
        Topic topic = new Topic("/a");
        TopicToken topicToken = new TopicToken("#");
        Assert.assertTrue(TopicTokenUtil.match(topic.getTopicToken(), topicToken));

        topic = new Topic("/a");
        topicToken = new TopicToken("/#");
        Assert.assertTrue(TopicTokenUtil.match(topic.getTopicToken(), topicToken));

        topic = new Topic("/a");
        topicToken = new TopicToken("+/#");
        Assert.assertTrue(TopicTokenUtil.match(topic.getTopicToken(), topicToken));

        topic = new Topic("/a/b/c");
        topicToken = new TopicToken("/#");
        Assert.assertTrue(TopicTokenUtil.match(topic.getTopicToken(), topicToken));

        topic = new Topic("/a/b/c");
        topicToken = new TopicToken("#");
        Assert.assertTrue(TopicTokenUtil.match(topic.getTopicToken(), topicToken));

        topic = new Topic("/a/b/c");
        topicToken = new TopicToken("/a/b/c");
        Assert.assertTrue(TopicTokenUtil.match(topic.getTopicToken(), topicToken));

        topic = new Topic("/a/b/c");
        topicToken = new TopicToken("a/b/c");
        Assert.assertFalse(TopicTokenUtil.match(topic.getTopicToken(), topicToken));

        topic = new Topic("/a/b/c");
        topicToken = new TopicToken("/+/b/+");
        Assert.assertTrue(TopicTokenUtil.match(topic.getTopicToken(), topicToken));

        topic = new Topic("/a");
        topicToken = new TopicToken("/+");
        Assert.assertTrue(TopicTokenUtil.match(topic.getTopicToken(), topicToken));

        topic = new Topic("/a");
        topicToken = new TopicToken("+/+");
        Assert.assertTrue(TopicTokenUtil.match(topic.getTopicToken(), topicToken));
    }

}
