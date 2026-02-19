package tech.smartboot.mqtt.plugin.openapi.controller;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.agent.FeatAgent;
import tech.smartboot.feat.ai.agent.ToolCaller;
import tech.smartboot.feat.ai.agent.hook.Hook;
import tech.smartboot.feat.ai.agent.tools.McpTool;
import tech.smartboot.feat.ai.chat.ChatModelVendor;
import tech.smartboot.feat.ai.chat.entity.Message;
import tech.smartboot.feat.ai.mcp.client.McpClient;
import tech.smartboot.feat.cloud.annotation.Autowired;
import tech.smartboot.feat.cloud.annotation.Controller;
import tech.smartboot.feat.cloud.annotation.Param;
import tech.smartboot.feat.cloud.annotation.PostConstruct;
import tech.smartboot.feat.cloud.annotation.PreDestroy;
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
import java.util.ArrayList;
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
    private final List<McpClient> mcpList = new ArrayList<>();

    @PostConstruct
    public void init() {
        // 初始化mcp
        if (pluginConfig.getOpenai() != null && FeatUtils.isNotEmpty(pluginConfig.getOpenai().getMcp())) {
            for (PluginConfig.Mcp mcp : pluginConfig.getOpenai().getMcp()) {
                McpClient mcpClient = McpClient.streamable(opt -> opt.debug(true).url(mcp.getUrl()));
                mcpClient.asyncInitialize().thenAccept(rsp -> {
                    mcpClient.asyncListTools(null).thenAccept(list -> {
                        mcpList.add(mcpClient);
                    });
                });
            }
        }
    }

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

        FeatAgent agent = FeatAI.agent(options -> options
//                .tool(new SearchTool())
//                .tool(new WebPageReaderTool())
                .chatOptions().system("你需要为用户提供关于 smart-mqtt 相关的专业性答疑服务，如果用户提问内容与本产品或者MQTT、物联网等无关，要给出提醒。\n" + "- [产品官网](https://smartboot.tech/smart-mqtt/)获取相关内容。\n" + "- [Gitee仓库](https://gitee.com/smartboot/smart-mqtt/)\n" + "- [smart-mqtt llms.txt](https://smartboot.tech/smart-mqtt/llms.txt)\n").model(new ChatModelVendor(openAI.getUrl(), openAI.getModel())).apiKey(openAI.getApiKey()));
        mcpList.forEach(mcp -> McpTool.register(agent, mcp));
        request.upgrade(new SSEUpgrade() {

            @Override
            public void onOpen(SseEmitter sseEmitter) {
                agent.options().hook(new Hook() {
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

    @PreDestroy
    public void destroy() {
        for (McpClient mcpClient : mcpList) {
            mcpClient.close();
        }
    }

    public void setPluginConfig(PluginConfig pluginConfig) {
        this.pluginConfig = pluginConfig;
    }
}