/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package tech.smartboot.mqtt.plugin.flapping;

import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
import tech.smartboot.mqtt.plugin.PluginConfig;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * 连接防抖检测器
 * 用于检测和限制频繁连接/断开的客户端（抖动客户端）
 * <p>
 * 参考 EMQX 的 flapping 检测机制：
 * - 在指定时间窗口内统计客户端的连接次数
 * - 超过阈值则判定为抖动客户端并予以封禁
 * - 封禁期间拒绝该客户端的连接请求
 *
 * @author 三刀
 * @version v1.0 2026/4/2
 */
public class FlappingDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlappingDetector.class);

    /**
     * 客户端连接事件记录
     * Key: 客户端标识（clientId 或 IP地址）
     * Value: 连接时间戳列表
     */
    private final Map<String, ConcurrentLinkedDeque<Long>> connectionRecords = new ConcurrentHashMap<>();

    /**
     * 封禁列表
     * Key: 客户端标识
     * Value: 封禁截止时间
     */
    private final Map<String, Long> bannedClients = new ConcurrentHashMap<>();

    /**
     * 防抖配置
     */
    private final PluginConfig.FlappingConfig config;

    public FlappingDetector(PluginConfig.FlappingConfig config) {
        this.config = config;
    }

    /**
     * 检查客户端是否被封禁
     *
     * @param clientId 客户端ID
     * @return 如果客户端被封禁返回 true，否则返回 false
     */
    public boolean isBanned(String clientId) {
        Long banEndTime = bannedClients.get(clientId);
        if (banEndTime == null) {
            return false;
        }
        if (System.currentTimeMillis() < banEndTime) {
            LOGGER.warn("客户端 {} 处于封禁状态，拒绝连接", clientId);
            return true;
        }

        // 封禁已过期，移除封禁记录
        bannedClients.remove(clientId);
        LOGGER.info("客户端 {} 封禁已解除", clientId);
        return false;
    }

    /**
     * 记录连接事件并检测是否为抖动客户端
     *
     * @param clientId 客户端ID
     */
    public void recordConnection(String clientId) {
        long currentTime = System.currentTimeMillis();
        long thresholdDurationMs = TimeUnit.SECONDS.toMillis(config.getThresholdDuration());

        // 获取或创建该客户端的连接记录
        ConcurrentLinkedDeque<Long> records = connectionRecords.computeIfAbsent(clientId, k -> new ConcurrentLinkedDeque<>());

        // 移除超出时间窗口的旧记录
        synchronized (records) {
            records.removeIf(timestamp -> currentTime - timestamp > thresholdDurationMs);

            // 添加当前连接时间戳
            records.addLast(currentTime);

            // 检查是否超过阈值
            if (records.size() > config.getThresholdCount()) {
                // 判定为抖动客户端，予以封禁
                long banEndTime = currentTime + TimeUnit.SECONDS.toMillis(config.getBanTime());
                bannedClients.put(clientId, banEndTime);
                connectionRecords.remove(clientId);

                LOGGER.warn("客户端 {} 在 {} 秒内连接了 {} 次，超过阈值 {} 次，判定为抖动客户端，封禁 {} 秒",
                        clientId, config.getThresholdDuration(), records.size(),
                        config.getThresholdCount(), config.getBanTime());
            }
        }
    }


    /**
     * 清理过期的记录
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        long thresholdDurationMs = TimeUnit.SECONDS.toMillis(config.getThresholdDuration());

        // 清理过期的封禁记录
        Iterator<Map.Entry<String, Long>> banIterator = bannedClients.entrySet().iterator();
        while (banIterator.hasNext()) {
            Map.Entry<String, Long> entry = banIterator.next();
            if (currentTime >= entry.getValue()) {
                banIterator.remove();
                LOGGER.info("客户端 {} 封禁已自动解除", entry.getKey());
            }
        }

        // 清理过期的连接记录
        Iterator<Map.Entry<String, ConcurrentLinkedDeque<Long>>> recordIterator = connectionRecords.entrySet().iterator();
        while (recordIterator.hasNext()) {
            Map.Entry<String, ConcurrentLinkedDeque<Long>> entry = recordIterator.next();
            ConcurrentLinkedDeque<Long> records = entry.getValue();
            synchronized (records) {
                records.removeIf(timestamp -> currentTime - timestamp > thresholdDurationMs);
                if (records.isEmpty()) {
                    recordIterator.remove();
                }
            }
        }
    }
}