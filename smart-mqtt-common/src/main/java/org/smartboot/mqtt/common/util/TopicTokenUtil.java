/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

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
