/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/6/4
 */module smart.mqtt.common {
    requires transitive aio.pro;
    requires  fastjson;
    requires org.apache.commons.collections4;
    requires commons.lang;
    requires org.slf4j;

    exports org.smartboot.mqtt.common;
    exports org.smartboot.mqtt.common.enums;
    exports org.smartboot.mqtt.common.message;
    exports org.smartboot.mqtt.common.listener;
    exports org.smartboot.mqtt.common.util;
    exports org.smartboot.mqtt.common.protocol;
    exports org.smartboot.mqtt.common.exception;
}