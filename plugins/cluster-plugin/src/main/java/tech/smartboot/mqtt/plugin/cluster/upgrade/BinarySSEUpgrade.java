package tech.smartboot.mqtt.plugin.cluster.upgrade;

import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.HttpResponse;
import tech.smartboot.feat.core.server.impl.Upgrade;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author 三刀
 * @version v1.0 6/23/25
 */
public abstract class BinarySSEUpgrade extends Upgrade {
    SseEmitter sseEmitter;

    @Override
    public void init(HttpRequest request, HttpResponse response) throws IOException {
        response = request.getResponse();
        response.getOutputStream().disableChunked();
        response.setHeader("Content-Type", "text/event-stream");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.getOutputStream().flush();
        SseEmitter sseEmitter = new SseEmitter(this.request.getAioSession());
        onOpen(sseEmitter);
    }

    @Override
    public void onBodyStream(ByteBuffer buffer) {

    }

    public abstract void onOpen(SseEmitter sseEmitter) throws IOException;

    @Override
    public void destroy() {
        if (sseEmitter != null) {
            sseEmitter.complete();
        }
    }
}
