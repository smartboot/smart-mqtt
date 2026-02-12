package tech.smartboot.mqtt.plugin.openapi.to;

/**
 * @author 三刀
 * @version v1.0 2/12/26
 */
public class AiChunkTO {
    public static final String TYPE_REASON = "reason";
    public static final String TYPE_TOOL_CALL = "tool_call";
    public static final String TYPE_RESULT = "result";
    private String type;
    private String data;

    public static AiChunkTO ofReason(String reason) {
        AiChunkTO aiChunkTO = new AiChunkTO();
        aiChunkTO.setType(TYPE_REASON);
        aiChunkTO.setData(reason);
        return aiChunkTO;
    }

    public static AiChunkTO ofToolCall(String toolCall) {
        AiChunkTO aiChunkTO = new AiChunkTO();
        aiChunkTO.setType(TYPE_TOOL_CALL);
        aiChunkTO.setData(toolCall);
        return aiChunkTO;
    }

    public static AiChunkTO ofResult(String result) {
        AiChunkTO aiChunkTO = new AiChunkTO();
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

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
