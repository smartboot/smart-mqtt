package org.smartboot.mqtt.broker.openapi;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2023/1/22
 */
public class OpenApi {
    public static final String BASE_API = "/api";

    public static final String DASHBOARD = BASE_API + "/dashboard";
    public static final String DASHBOARD_OVERVIEW = DASHBOARD + "/overview";

    public static final String DASHBOARD_NODES = DASHBOARD + "/nodes";

    public static final String DASHBOARD_METRICS = DASHBOARD + "/metrics";

    public static final String CONNECTIONS = BASE_API + "/connections";

    public static final String SUBSCRIPTIONS = BASE_API + "/subscriptions";

    public static final String SUBSCRIPTIONS_SUBSCRIPTION = SUBSCRIPTIONS + "/subscription";
    public static final String SUBSCRIPTIONS_TOPICS = SUBSCRIPTIONS + "/topics";

    public static final String MESSAGE_UPGRADE = "升级企业版解锁此功能!";
}
