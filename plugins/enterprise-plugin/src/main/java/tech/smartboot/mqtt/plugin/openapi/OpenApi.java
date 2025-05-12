/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.openapi;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
public class OpenApi {
    public static final String BASE_API = "/api";

    public static final String DASHBOARD = BASE_API + "/dashboard";

    public static final String DASHBOARD_NODES = DASHBOARD + "/nodes";

    public static final String CONNECTIONS = BASE_API + "/connections";

    /**
     * 断开连接
     */
    public static final String DIS_CONNECTION = BASE_API + "/disconnection";

    public static final String SUBSCRIPTIONS = BASE_API + "/subscriptions";

    /**
     * 获取Broker列表
     */
    public static final String BROKERS = BASE_API + "/brokers";

    public static final String SUBSCRIPTIONS_SUBSCRIPTION = SUBSCRIPTIONS + "/subscription";
    public static final String SUBSCRIPTIONS_TOPICS = SUBSCRIPTIONS + "/topics";


    /**
     * 系统设置
     */
    public static final String SYSTEM = BASE_API + "/system";

    public static final String SYSTEM_LICENSE = SYSTEM + "/license";

    public static final String SYSTEM_VERSION = SYSTEM + "/version";

    public static final String SYSTEM_USER_LIST = SYSTEM + "/user/list";

    public static final String SYSTEM_USER_ADD = SYSTEM + "/user/add";

    public static final String SYSTEM_USER_DELETE = SYSTEM + "/user/delete";

    public static final String SYSTEM_SETTINGS_GET = SYSTEM + "/settings/get";

    public static final String SYSTEM_SETTINGS_SAVE = SYSTEM + "/settings/save";

    /**
     * 用户管理
     */
    private static final String USER = BASE_API + "/user";
    public static final String USER_LOGIN = USER + "/login";
    /**
     * License管理
     */
    private static final String LICENSE = BASE_API + "/license";
    public static final String LICENSE_IMPORT = LICENSE + "/import";
    public static final String LICENSE_GET = LICENSE + "/get";

    public static final String MESSAGE_UPGRADE = "升级企业版解锁此功能!";
}
