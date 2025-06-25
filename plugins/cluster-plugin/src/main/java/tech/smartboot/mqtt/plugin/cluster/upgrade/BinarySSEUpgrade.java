package tech.smartboot.mqtt.plugin.cluster.upgrade;

import org.smartboot.socket.util.StringUtils;
import tech.smartboot.feat.core.common.logging.Logger;
import tech.smartboot.feat.core.common.logging.LoggerFactory;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(BinarySSEUpgrade.class);
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
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        LOGGER.error("BinarySSEUpgrade.onBodyStream:{}", StringUtils.toHexString(bytes));
    }

    public abstract void onOpen(SseEmitter sseEmitter) throws IOException;

    @Override
    public void destroy() {
        if (sseEmitter != null) {
            sseEmitter.complete();
        }
    }
}
