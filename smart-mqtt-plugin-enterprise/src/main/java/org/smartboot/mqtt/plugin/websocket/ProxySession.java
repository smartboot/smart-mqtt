package org.smartboot.mqtt.plugin.websocket;

import org.smartboot.socket.transport.AioSession;
import org.smartboot.socket.transport.WriteBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/12/2
 */
class ProxySession extends AioSession {
    private final AioSession session;

    public ProxySession(AioSession session) {
        this.session = session;
    }

    @Override
    public WriteBuffer writeBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer readBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void awaitRead() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void signalRead() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close(boolean immediate) {
        session.close(immediate);
    }

    @Override
    public InetSocketAddress getLocalAddress() throws IOException {
        return session.getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() throws IOException {
        return session.getRemoteAddress();
    }

    @Override
    public String getSessionID() {
        return session.getSessionID();
    }

    @Override
    public boolean isInvalid() {
        return session.isInvalid();
    }

}
