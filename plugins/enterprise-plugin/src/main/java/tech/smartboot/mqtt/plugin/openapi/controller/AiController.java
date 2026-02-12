package tech.smartboot.mqtt.plugin.openapi.controller;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.agent.FeatAgent;
import tech.smartboot.feat.ai.agent.ToolCaller;
import tech.smartboot.feat.ai.agent.hook.Hook;
import tech.smartboot.feat.ai.agent.tools.SearchTool;
import tech.smartboot.feat.ai.agent.tools.WebPageReaderTool;
import tech.smartboot.feat.ai.chat.ChatModelVendor;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.RequestMapping;
import tech.smartboot.feat.core.common.FeatUtils;
import tech.smartboot.feat.core.common.exception.FeatException;
import tech.smartboot.feat.core.server.HttpRequest;
import tech.smartboot.feat.core.server.upgrade.sse.SSEUpgrade;
import tech.smartboot.feat.core.server.upgrade.sse.SseEmitter;
import tech.smartboot.mqtt.plugin.PluginConfig;
import tech.smartboot.mqtt.plugin.openapi.OpenApi;
import tech.smartboot.mqtt.plugin.openapi.to.AiChunkTO;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author 三刀
 * @version v1.0 2/10/26
 */
@Controller
public class AiController {

    @Autowired
    private PluginConfig pluginConfig;

    @RequestMapping(value = OpenApi.BASE_API + "/chat/completions")
    public void chat(HttpRequest request, @Param("messages") List<Message> messages) throws IOException {
        PluginConfig.OpenAI openAI = pluginConfig.getOpenai();
        if (openAI == null || FeatUtils.isBlank(openAI.getUrl())) {
            throw new FeatException("配置为空");
        }
        if (FeatUtils.isBlank(openAI.getModel())) {
            throw new FeatException("模型为空");
        }
        if (FeatUtils.isEmpty(messages)) {
            throw new FeatException("消息为空");
        }
        StringBuilder sb = new StringBuilder();
        for (Message message : messages) {
            sb.append(message.getRole()).append(": ").append(message.getContent()).append("\n");
        }
        FeatAgent agent = FeatAI.agent(agentOptions -> agentOptions.addTool(new SearchTool()).addTool(new WebPageReaderTool()).chatOptions().system("你需要为用户提供关于 smart-mqtt 相关的专业性答疑服务，可从[产品官网](https://smartboot.tech/smart-mqtt/)获取相关内容。如果用户提问内容与本产品或者MQTT、物联网等无关，要给出提醒").model(new ChatModelVendor(openAI.getUrl(), openAI.getModel())).apiKey(openAI.getApiKey()));
        request.upgrade(new SSEUpgrade() {

            @Override
            public void onOpen(SseEmitter sseEmitter) {
                agent.options().hook(new Hook() {
                    @Override
                    public void preCall(List<Message> message) {
                        Hook.super.preCall(message);
                    }

                    @Override
                    public void postCall(Message message) {
                        Hook.super.postCall(message);
                    }

                    @Override
                    public void preTool(ToolCaller toolCaller) {
                        sseEmitter.sendAsJson(AiChunkTO.ofToolCall(toolCaller));
                    }

                    @Override
                    public void postTool(ToolCaller toolCaller) {
                        sseEmitter.sendAsJson(AiChunkTO.ofToolCall(toolCaller));
                    }

                    @Override
                    public void onReasoning(String agentAction) {
                        sseEmitter.sendAsJson(AiChunkTO.ofReason(agentAction));
                    }
                });
                CompletableFuture<String> completableFuture = agent.execute(sb.toString());
                completableFuture.thenAccept(result -> {

                    try {
                        sseEmitter.sendAsJson(AiChunkTO.ofResult(result));
                    } finally {
                        sseEmitter.complete();
                    }
                }).exceptionally(throwable -> {
                    try {
                        sseEmitter.sendAsJson(AiChunkTO.ofResult(throwable.getMessage()));
                    } finally {
                        sseEmitter.complete();
                    }
                    return null;
                });
            }

            @Override
            public void destroy() {
                agent.cancel();
            }
        });

    }

    public void setPluginConfig(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }
}
