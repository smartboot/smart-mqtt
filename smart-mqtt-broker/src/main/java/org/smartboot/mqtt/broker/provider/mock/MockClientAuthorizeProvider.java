package org.smartboot.mqtt.broker.provider.mock;

import org.smartboot.mqtt.broker.provider.ClientAuthorizeProvider;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public class MockClientAuthorizeProvider implements ClientAuthorizeProvider {
    @Override
    public boolean auth(String userName, String clientIdentifier, byte[] password) {
        return true;
    }
}
