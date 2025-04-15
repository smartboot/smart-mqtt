/*
 * Copyright (C) [2022] smartboot [zhengjunweimail@163.com]
 *
 *  企业用户未经smartboot组织特别许可，需遵循AGPL-3.0开源协议合理合法使用本项目。
 *
 *  Enterprise users are required to use this project reasonably
 *  and legally in accordance with the AGPL-3.0 open source agreement
 *  without special permission from the smartboot organization.
 */

package org.smartboot.mqtt.broker.plugin.ws;

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
