package tech.smartboot.mqtt.common;

/**
 * @author 三刀
 * @version v1.0 6/19/25
 */
public class TopicNode {
    public static final TopicNode WILDCARD_HASH_NODE = new TopicNode(0, 1, "#");
    public static final TopicNode WILDCARD_PLUS_NODE = new TopicNode(0, 1, "+");
    public static final TopicNode SHARE_NODE = new TopicNode(0, "$share".length(), "$share");
    private final int begin;
    private final int end;
    private final String node;
    private int hash;

    public TopicNode(int begin, int end, String node) {
        this.begin = begin;
        this.end = end;
        this.node = node;
    }

    public boolean contains(char c) {
        for (int i = begin; i < end; i++) {
            if (node.charAt(i) == c) {
                return true;
            }
        }
        return false;
    }

    public int length() {
        return end - begin;
    }

    @Override
    public int hashCode() {
        int h = hash;
        if (h == 0 && length() > 0) {
            for (int i = begin, j = 0; i < end; i++, j++) {
                h = 31 * h + node.charAt(i);
            }
            hash = h;
        }
        return h;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TopicNode) {
            TopicNode anotherString = (TopicNode) obj;
            int n = length();
            if (n == anotherString.length()) {
                for (int i = begin, j = anotherString.begin; i < end; i++, j++) {
                    if (node.charAt(i) != anotherString.node.charAt(j)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
