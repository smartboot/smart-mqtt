package org.smartboot.mqtt.broker.eventbus;

import org.apache.commons.lang.StringUtils;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttConnectMessage;
import org.smartboot.mqtt.common.util.ValidateUtils;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/7/16
 */
public class ConnectAuthenticationSubscriber implements EventBusSubscriber<EventObject<MqttConnectMessage>> {
    private final BrokerContext context;

    public ConnectAuthenticationSubscriber(BrokerContext context) {
        this.context = context;
    }

    @Override
    public void subscribe(EventType<EventObject<MqttConnectMessage>> eventType, EventObject<MqttConnectMessage> object) {
        String validUserName = context.getBrokerConfigure().getUsername();
        if (StringUtils.isBlank(validUserName)) {
            object.getSession().setAuthorized(true);
            return;
        }
        String userName = object.getObject().getPayload().userName();
        String password = new String(object.getObject().getPayload().passwordInBytes());
        //身份验证
        ValidateUtils.isTrue(StringUtils.equals(validUserName, userName)
                        && (StringUtils.isBlank(context.getBrokerConfigure().getPassword())
                        || StringUtils.equals(password, context.getBrokerConfigure().getPassword()))
                , "login fail", object.getSession()::disconnect);
        object.getSession().setAuthorized(true);
        object.getSession().setUsername(userName);
    }
}
