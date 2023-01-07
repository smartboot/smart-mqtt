package org.smartboot.mqtt.common;

import java.io.IOException;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/2
 */
public interface MqttWriter {
    void writeByte(byte b);

    void writeShort(short data) throws IOException;

    void writeInt(int data) throws IOException;

    void write(byte[] data) throws IOException;

    void flush();
}
