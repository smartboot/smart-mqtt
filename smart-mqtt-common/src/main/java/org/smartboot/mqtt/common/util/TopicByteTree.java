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
public class TopicByteTree {
    private static final int MAX_DEPTH = 128;
    private final int depth;
    private final TopicByteTree parent;
    protected String topicName;
    private byte[] bytes;
    private int shift = -1;
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

    public TopicByteTree search(ByteBuffer bytes, int length, boolean cache) {
        return search(bytes, 0, length, cache);
    }

    /**
     * 从给定的字节数组总匹配出特定结尾的区块
     *
     * @param bytes      待匹配的字节数组
     * @param cache      是否缓存新节点
     * @return
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
                topicByteTree.topicName = new String(b, 2, len);
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

    public void addTopic(String value) {
        byte[] bytes = value.getBytes();
        TopicByteTree tree = this;
        while (tree.depth > 0) {
            tree = tree.parent;
        }
        TopicByteTree leafNode = tree.addTopic(bytes, 0, bytes.length);
        leafNode.topicName = value;
    }

    private TopicByteTree addTopic(byte[] value, int offset, int len) {
        if (offset == len) {
            byte[] b = new byte[len + 2];
            b[0] = (byte) ((len >>> 8) & 0xFF);
            b[1] = (byte) (len & 0xFF);
            System.arraycopy(value, 0, b, 2, len);
            bytes = b;
            topicName = new String(b, 2, len);
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


    private static class VirtualTopicByteTree extends TopicByteTree {
        public VirtualTopicByteTree(byte[] b, String s) {
            super();
            super.topicName = s;
            super.bytes = b;
        }
    }
}
