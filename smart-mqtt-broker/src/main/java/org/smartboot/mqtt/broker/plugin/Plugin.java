package org.smartboot.mqtt.broker.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.mqtt.broker.BrokerContext;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/4/1
 */
public abstract class Plugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(Plugin.class);
    /**
     * 是否已安装
     */
    private boolean installed;
    /**
     * 插件名称
     */
    private String pluginName;

    /**
     * 获取插件名称
     *
     * @return
     */
    public String pluginName() {
        if (pluginName == null) {
            pluginName = this.getClass().getSimpleName();
        }
        return pluginName;
    }


    /**
     * 安装插件,需要在servlet服务启动前调用
     */
    public final void install(BrokerContext brokerContext) {
        checkSate();
        initPlugin(brokerContext);
        installed = true;
    }

    /**
     * 初始化插件
     */
    protected void initPlugin(BrokerContext brokerContext) {
        LOGGER.info("plugin:[" + pluginName() + "] do nothing when initPlugin!");
    }

    /**
     * 卸载插件,在容器服务停止前调用
     */
    public final void uninstall() {
        destroyPlugin();
    }

    /**
     * 销毁插件
     */
    protected void destroyPlugin() {
        LOGGER.info("plugin:[" + pluginName() + "] do nothing when destroyPlugin!");
    }

    private void checkSate() {
        if (installed) {
            throw new IllegalStateException("plugin [ " + pluginName() + " ] has installed!");
        }
    }

    /**
     * 插件排序值，数值小的被优先加载
     */
    public int order() {
        return 0;
    }
}
