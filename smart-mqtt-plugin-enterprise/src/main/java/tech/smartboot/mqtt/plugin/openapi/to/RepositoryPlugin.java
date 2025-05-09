/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi.to;

import java.util.List;

/**
 * @author 三刀
 * @version v1.0 4/23/25
 */
public class RepositoryPlugin {
    public static final String REPOSITORY = "repository";
    public static final String REPOSITORY_PLUGIN_NAME = "plugin.jar";
    private List<PluginTO> plugins;

    public List<PluginTO> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<PluginTO> plugins) {
        this.plugins = plugins;
    }
}
