package org.smartboot.mqtt.common.util;

import org.apache.commons.lang.StringUtils;
import org.smartboot.mqtt.common.TopicToken;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/3
 */
public class TopicTokenUtil {
    public static boolean match(TopicToken pubTopicToken, TopicToken subTopicToken) {
        if (subTopicToken == null) {
            return pubTopicToken == null;
        }
        //合法的#通配符必然存在于末端
        if ("#".equals(subTopicToken.getNode())) {
            return true;
        }
        if ("+".equals(subTopicToken.getNode())) {
            return pubTopicToken != null && match(pubTopicToken.getNextNode(), subTopicToken.getNextNode());
        }
        if (pubTopicToken == null || !StringUtils.equals(pubTopicToken.getNode(), subTopicToken.getNode())) {
            return false;
        }
        return match(pubTopicToken.getNextNode(), subTopicToken.getNextNode());
    }
}
