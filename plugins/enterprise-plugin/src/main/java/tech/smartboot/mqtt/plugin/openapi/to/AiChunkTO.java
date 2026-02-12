package tech.smartboot.mqtt.plugin.openapi.to;

import tech.smartboot.feat.ai.agent.ToolCaller;

/**
 * @author 三刀
 * @version v1.0 2/12/26
 */
public class AiChunkTO<T> {
    public static final String TYPE_REASON = "reason";
    public static final String TYPE_TOOL_CALL = "tool_call";
    public static final String TYPE_RESULT = "result";
    private String type;
    private T data;

    public static AiChunkTO<String> ofReason(String reason) {
        AiChunkTO<String> aiChunkTO = new AiChunkTO<>();
        aiChunkTO.setType(TYPE_REASON);
        aiChunkTO.setData(reason);
        return aiChunkTO;
    }

    public static AiChunkTO<ToolCaller> ofToolCall(ToolCaller toolCall) {
        AiChunkTO<ToolCaller> aiChunkTO = new AiChunkTO<>();
        aiChunkTO.setType(TYPE_TOOL_CALL);
        aiChunkTO.setData(toolCall);
        return aiChunkTO;
    }

    public static AiChunkTO<String> ofResult(String result) {
        AiChunkTO<String> aiChunkTO = new AiChunkTO<>();
        aiChunkTO.setType(TYPE_RESULT);
        aiChunkTO.setData(result);
        return aiChunkTO;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
