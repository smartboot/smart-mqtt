/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.common.util;

import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/2
 */
/**
 * 基于字节数组的MQTT主题树实现，用于高效存储和匹配主题。
 * <p>
 * 该类通过字节级别的树形结构来存储和匹配MQTT主题，具有以下特点：
 * <ul>
 *   <li>高效性 - 使用字节数组而非字符串进行主题存储和匹配</li>
 *   <li>内存优化 - 采用紧凑的数组结构存储节点</li>
 *   <li>通配符支持 - 完整支持MQTT的主题通配符（+和#）</li>
 *   <li>动态扩展 - 支持动态调整节点数组大小</li>
 * </ul>
 * </p>
 * <p>
 * 主要应用场景：
 * <ul>
 *   <li>主题订阅树 - 存储客户端的主题订阅关系</li>
 *   <li>主题匹配 - 快速查找匹配的订阅者</li>
 *   <li>路由转发 - 支持基于主题的消息路由</li>
 * </ul>
 * </p>
 */
public class TopicByteTree {
    /** 主题树的最大深度限制，防止无限递归 */
    private static final int MAX_DEPTH = 128;

    /** 当前节点在树中的深度 */
    private final int depth;

    /** 父节点引用，用于树的遍历 */
    private final TopicByteTree parent;

    /** 当前节点对应的主题名称 */
    protected String topicName;

    /** 
     * 标识当前主题是否包含通配符（+或#）
     * 用于优化主题匹配过程
     */
    private boolean wildcards;

    /** 存储主题对应的原始字节数组 */
    private byte[] bytes;

    /** 
     * 字节偏移量，用于优化节点数组的空间利用
     * -1表示尚未初始化
     */
    private int shift = -1;

    /** 
     * 子节点数组，采用数组而非Map以优化性能
     * 数组索引 = 字节值 - shift
     */
    private TopicByteTree[] nodes = new TopicByteTree[1];


    public TopicByteTree() {
        this(null);
    }

    public TopicByteTree(TopicByteTree parent) {
        this.parent = parent;
        this.depth = parent == null ? 0 : parent.depth + 1;
        if (depth > MAX_DEPTH) {
            throw new IllegalStateException("maxDepth is " + MAX_DEPTH + " , current is " + depth);
        }
    }

    public byte[] getBytes() {
        return bytes;
    }

    /**
     * 在主题树中搜索匹配的主题节点
     *
     * @param bytes 待匹配的主题字节缓冲区
     * @param length 主题的字节长度
     * @param cache 是否缓存新建的节点
     * @return 匹配到的主题节点，如果未找到则返回虚拟节点
     */
    public TopicByteTree search(ByteBuffer bytes, int length, boolean cache) {
        return search(bytes, 0, length, cache);
    }

    /**
     * 在主题树中搜索匹配的主题节点，支持指定偏移量和长度
     * <p>
     * 搜索过程：
     * <ol>
     *   <li>从根节点开始，逐字节匹配主题路径</li>
     *   <li>对每个字节，计算在当前节点数组中的索引位置</li>
     *   <li>如果找到对应节点，继续匹配下一个字节</li>
     *   <li>如果未找到节点，根据cache参数决定是否创建新节点</li>
     * </ol>
     * </p>
     *
     * @param bytes 待匹配的主题字节缓冲区
     * @param offset 起始偏移量
     * @param len 要匹配的字节长度
     * @param cache 是否缓存新建的节点
     * @return 匹配到的主题节点，如果未找到则返回虚拟节点
     */
    public TopicByteTree search(ByteBuffer bytes, int offset, int len, boolean cache) {
        TopicByteTree topicByteTree = this;
        while (offset < len) {
            int i = bytes.get() - topicByteTree.shift;
            if (i >= topicByteTree.nodes.length || i < 0) {
                break;
            }
            TopicByteTree b = topicByteTree.nodes[i];
            if (b != null) {
                topicByteTree = b;
                offset++;
            } else {
                break;
            }
        }
        if (offset == len) {
            if (topicByteTree.topicName == null) {
                //在当前节点上追加子节点
                byte[] b = new byte[len + 2];
                bytes.position(bytes.position() - offset - 2);
                bytes.get(b);
                topicByteTree.bytes = b;
                topicByteTree.setTopicName(new String(b, 2, len));
            }
            return topicByteTree;
        } else if (cache && topicByteTree.depth < MAX_DEPTH) {
            //在当前节点上追加子节点
            int p = bytes.position() - 1;
            byte[] b = new byte[len];
            bytes.position(p - offset);
            bytes.get(b);
            bytes.position(p);
            topicByteTree.addTopic(b, offset, len);
            return topicByteTree.search(bytes, offset, len, cache);
        } else {
            //在当前节点上追加子节点
            byte[] b = new byte[len + 2];
            bytes.position(bytes.position() - offset - 3);
            bytes.get(b);
            return new VirtualTopicByteTree(b, new String(b, 2, len));
        }
    }

    /**
     * 向主题树中添加新的主题
     * <p>
     * 添加过程：
     * <ol>
     *   <li>如果到达主题末尾，创建主题节点</li>
     *   <li>如果未到达末尾，递归添加剩余部分</li>
     *   <li>动态调整节点数组大小以容纳新节点</li>
     * </ol>
     * </p>
     *
     * @param value 主题的字节数组
     * @param offset 当前处理的字节偏移量
     * @param len 主题的总字节长度
     * @return 添加的主题节点
     */
    private synchronized TopicByteTree addTopic(byte[] value, int offset, int len) {
        if (offset == len) {
            byte[] b = new byte[len + 2];
            b[0] = (byte) ((len >>> 8) & 0xFF);
            b[1] = (byte) (len & 0xFF);
            System.arraycopy(value, 0, b, 2, len);
            bytes = b;
            setTopicName(new String(b, 2, len));
            return this;
        }
        if (this.depth >= MAX_DEPTH) {
            return this;
        }

        byte b = value[offset++];
        if (shift == -1) {
            shift = b;
        }
        if (b - shift < 0) {
            increase(b - shift);
        } else {
            increase(b + 1 - shift);
        }

        TopicByteTree nextTree = nodes[b - shift];
        if (nextTree == null) {
            nextTree = nodes[b - shift] = new TopicByteTree(this);
        }
        return nextTree.addTopic(value, offset, len);
    }

    /**
     * 调整节点数组的大小以适应新的节点
     * <p>
     * 调整策略：
     * <ul>
     *   <li>负值：向左扩展数组，调整shift值</li>
     *   <li>正值：向右扩展数组</li>
     *   <li>特殊处理size=0的情况</li>
     * </ul>
     * </p>
     *
     * @param size 需要的数组大小调整量
     */
    private void increase(int size) {
        if (size == 0) size = -1;
        if (size < 0) {
            TopicByteTree[] temp = new TopicByteTree[nodes.length - size];
            System.arraycopy(nodes, 0, temp, -size, nodes.length);
            nodes = temp;
            shift += size;
        } else if (nodes.length < size) {
            TopicByteTree[] temp = new TopicByteTree[size];
            System.arraycopy(nodes, 0, temp, 0, nodes.length);
            nodes = temp;
        }
    }

    public String getTopicName() {
        return topicName;
    }

    public boolean isWildcards() {
        return wildcards;
    }

    void setTopicName(String topicName) {
        this.topicName = topicName;
        wildcards = MqttUtil.containsTopicWildcards(topicName);
    }

    /**
     * 虚拟主题节点，用于表示未缓存的临时主题
     * <p>
     * 主要用于：
     * <ul>
     *   <li>避免在非缓存模式下创建实际节点</li>
     *   <li>提供临时的主题匹配结果</li>
     * </ul>
     * </p>
     */
    private static class VirtualTopicByteTree extends TopicByteTree {
        public VirtualTopicByteTree(byte[] b, String s) {
            super();
            super.setTopicName(s);
            super.bytes = b;
        }
    }
}
