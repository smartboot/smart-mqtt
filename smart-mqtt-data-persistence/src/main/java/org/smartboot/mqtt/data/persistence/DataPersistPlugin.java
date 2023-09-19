package org.smartboot.mqtt.data.persistence;
import org.smartboot.mqtt.broker.BrokerContext;
import org.smartboot.mqtt.broker.plugin.Plugin;

/**
* @Description: 数据持久化插件约定
 * @Author: learnhope
 * @Date: 2023/9/18
 */
public abstract class DataPersistPlugin<T> extends Plugin {
    private T config;
    
    public void setConfig(T config) {
        this.config = config;
    }
    
    public T getConfig() {
        return config;
    }
    @Override
    protected void initPlugin(BrokerContext brokerContext) {
        T config = connect(brokerContext);
        listenAndPushMessage(brokerContext, config);
    }
    protected abstract T connect(BrokerContext brokerContext);
    protected abstract void listenAndPushMessage(BrokerContext brokerContext, T config);
}
