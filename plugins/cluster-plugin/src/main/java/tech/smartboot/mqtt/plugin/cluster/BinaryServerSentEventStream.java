package tech.smartboot.mqtt.plugin.cluster;

import tech.smartboot.feat.core.client.HttpResponse;
import tech.smartboot.feat.core.client.stream.Stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author 三刀
 * @version v1.0 6/23/25
 */
public abstract class BinaryServerSentEventStream implements Stream {
    private static final int STATE_TAG = 1;
    private static final int STATE_COLON = 2;
    private static final int STATE_COLON_RIGHT_TRIM = 3;
    private static final int STATE_LF = 4;
    private static final int STATE_END_CHECK = 5;
    private static final int STATE_PAYLOAD_LENGTH = 6;
    public static final int STATE_TAG_TOPIC = 7;
    public static final int STATE_TAG_RETAIN = 8;
    public static final int STATE_TAG_PAYLOAD = 9;
    public static final byte TAG_TOPIC = 't';
    public static final byte TAG_PAYLOAD = 'p';
    public static final byte TAG_RETAIN = 'r';
    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    /**
     * 负载数据
     */
    private byte[] payload;

    /**
     * 主题
     */
    private String topic;

    private boolean retained;

    @Override
    public void stream(HttpResponse response, byte[] bytes, boolean end) throws IOException {
        if (baos.size() > 0) {
            baos.write(bytes);
            bytes = baos.toByteArray();
            baos.reset();
        }
        int pos = 0;
        int valuePos = -1;

        int state = STATE_TAG;
        byte tag = 0;
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            switch (state) {
                case STATE_TAG:
                    tag = b;
                    state = STATE_COLON;
                    break;
                case STATE_COLON:
                    if (b == ':') {
                        state = STATE_COLON_RIGHT_TRIM;
                    }
                    break;
                case STATE_COLON_RIGHT_TRIM:
                    if (b != ' ') {
                        valuePos = i;
                        switch (tag) {
                            case TAG_TOPIC:
                                state = STATE_TAG_TOPIC;
                                break;
                            case TAG_PAYLOAD:
                                state = STATE_TAG_PAYLOAD;
                                break;
                            case TAG_RETAIN:
                                state = STATE_TAG_RETAIN;
                                break;
                            default:
                                throw new IllegalStateException("Unexpected value: " + tag);
                        }
                    }
                    break;
                case STATE_TAG_TOPIC:
                    if (b == '\n') {
                        topic = new String(bytes, valuePos, i - valuePos);
                        state = STATE_END_CHECK;
                        pos = i + 1;
                    }
                    break;
                case STATE_TAG_RETAIN:
                    if (b == '\n') {
                        retained = true;
                        state = STATE_END_CHECK;
                        pos = i + 1;
                    }
                    break;
                case STATE_TAG_PAYLOAD:
                    if (b == ' ') {
                        int length = Integer.parseInt(new String(bytes, valuePos, i - valuePos));
                        if (bytes.length - i > length) {
                            if (bytes[i + length] != '\n') {
                                throw new IllegalStateException("Unexpected value: " + tag);
                            }
                            byte[] payload = new byte[length];
                            System.arraycopy(bytes, i + 1, payload, 0, length);
                            this.payload = payload;
                            i = i + length + 1;
                            state = STATE_END_CHECK;
                        } else {
                            i = bytes.length;
                        }
                    }
                    break;

                case STATE_END_CHECK:
                    if (b == '\n') {
                        pos = i + 1;
                        //结束
//                        System.out.println(event);
                        onEvent(response, topic, payload, retained);
                        topic = null;
                        payload = null;
                        retained = false;
                    } else {
                        state = STATE_TAG;
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + state);
            }
        }

        if (pos < bytes.length) {
            baos.write(bytes, pos, bytes.length - pos);
        }
    }

    public abstract void onEvent(HttpResponse httpResponse, String topic, byte[] payload, boolean retained);
}
