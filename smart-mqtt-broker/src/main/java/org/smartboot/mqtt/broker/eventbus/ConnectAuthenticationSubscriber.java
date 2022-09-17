package org.smartboot.mqtt.broker.eventbus;

import org.smartboot.mqtt.broker.AuthenticationService;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.ConfiguredAuthenticationServiceImpl;
import org.smartboot.mqtt.common.eventbus.EventBusSubscriber;
import org.smartboot.mqtt.common.eventbus.EventType;
import org.smartboot.mqtt.common.message.MqttConnectMessage;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/7/16
 */
public class ConnectAuthenticationSubscriber implements EventBusSubscriber<EventObject<MqttConnectMessage>> {
    private final BrokerContext context;
    private final AuthenticationService authenticationService;

    public ConnectAuthenticationSubscriber(BrokerContext context) {
        this.context = context;
        // TODO Determine use which implements.
        this.authenticationService = new ConfiguredAuthenticationServiceImpl(context);
    }

    @Override
    public void subscribe(EventType<EventObject<MqttConnectMessage>> eventType, EventObject<MqttConnectMessage> object) {
//        String validUserName = context.getBrokerConfigure().getUsername();
//        if (StringUtils.isBlank(validUserName)) {
//            object.getSession().setAuthorized(true);
//            return;
//        }
        String userName = object.getObject().getPayload().userName();
        byte[] passwordBytes = object.getObject().getPayload().passwordInBytes();
        String password = passwordBytes == null ? "" : new String(passwordBytes);

        boolean result = authenticationService.authentication(userName, password, object.getSession());

//        //身份验证
//        ValidateUtils.isTrue(StringUtils.equals(validUserName, userName)
//                        && (StringUtils.isBlank(context.getBrokerConfigure().getPassword())
//                        || StringUtils.equals(password, context.getBrokerConfigure().getPassword()))
//                , "login fail", object.getSession()::disconnect);
        object.getSession().setAuthorized(result);
        object.getSession().setUsername(userName);
    }
}
