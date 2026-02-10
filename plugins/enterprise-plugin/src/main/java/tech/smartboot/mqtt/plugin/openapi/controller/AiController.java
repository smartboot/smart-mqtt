package tech.smartboot.mqtt.plugin.openapi.controller;

import tech.smartboot.feat.ai.FeatAI;
import tech.smartboot.feat.ai.agent.FeatAgent;
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
        FeatAgent agent = FeatAI.agent(agentOptions -> agentOptions.chatOptions()
                .model(new ChatModelVendor(openAI.getUrl(), openAI.getModel())).apiKey(openAI.getApiKey()));
        request.upgrade(new SSEUpgrade() {

            @Override
            public void onOpen(SseEmitter sseEmitter) {
                CompletableFuture<String> completableFuture = agent.execute(sb.toString());
                completableFuture.thenAccept(result -> {
                    try {
                        sseEmitter.send(result);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
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
