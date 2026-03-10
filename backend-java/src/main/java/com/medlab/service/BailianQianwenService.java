package com.medlab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BailianQianwenService {

    @Value("${ai.bailian.api-key}")
    private String apiKey;

    @Value("${ai.bailian.base-url}")
    private String baseUrl;

    @Value("${ai.bailian.model}")
    private String model;

    @Value("${ai.bailian.temperature:0.7}")
    private float temperature;

    @Value("${ai.bailian.top-p:0.95}")
    private float topP;

    @Value("${ai.bailian.max-tokens:2000}")
    private int maxTokens;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.builder().build();

    /**
     * 同步调用：供 AgentFuncRealize 调用
     */
    public String generateText(String prompt) {
        try {
            return generateTextStream(prompt)
                    .collectList()
                    .map(list -> String.join("", list))
                    .block(Duration.ofSeconds(60));
        } catch (Exception e) {
            log.error("AI Blocking call failed: {}", e.getMessage());
            return "AI 响应超时。";
        }
    }

    /**
     * 流式调用
     */
    public Flux<String> generateTextStream(String prompt) {
        try {
            String requestBody = buildRequestBody(prompt);
            // 重点：在这里观察控制台输出，确认 max_tokens 是否在根部
            log.info("Request Body: {}", requestBody);

            return webClient.post()
                    .uri(baseUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                    .map(event -> {
                        String data = event.data();
                        if (data == null || "[DONE]".equals(data)) return "";
                        return parseJsonContent(data);
                    })
                    .filter(content -> !content.isEmpty())
                    .onErrorResume(e -> {
                        log.error("Stream error: {}", e.getMessage());
                        return Flux.just("【AI 连接异常】");
                    });
        } catch (Exception e) {
            return Flux.error(new RuntimeException("AI 初始化失败"));
        }
    }

    /**
     * 多轮对话流式调用：接受完整的消息列表
     */
    public Flux<String> generateTextStreamWithMessages(List<Map<String, String>> chatMessages) {
        try {
            String requestBody = buildRequestBodyWithMessages(chatMessages);
            log.info("Multi-turn Request Body: {}", requestBody);

            return webClient.post()
                    .uri(baseUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                    .map(event -> {
                        String data = event.data();
                        if (data == null || "[DONE]".equals(data)) return "";
                        return parseJsonContent(data);
                    })
                    .filter(content -> !content.isEmpty())
                    .onErrorResume(e -> {
                        log.error("Stream error: {}", e.getMessage());
                        return Flux.just("【AI 连接异常】");
                    });
        } catch (Exception e) {
            return Flux.error(new RuntimeException("AI 初始化失败"));
        }
    }

    /**
     * 关键修正：将所有参数"平铺"到 JSON 根部
     */
    private String buildRequestBody(String prompt) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        
        // OpenAI 兼容模式要求这些必须在第一层
        root.put("model", model);
        root.put("stream", true);
        root.put("max_tokens", maxTokens); // 这里的 2000 才会生效
        root.put("temperature", temperature);
        root.put("top_p", topP);
        
        ArrayNode messages = root.putArray("messages");
        // 强制设置 MedLabAgent 身份，防止它"自我介绍"过短
        messages.addObject().put("role", "system").put("content", "你是一个专业的医疗 AI 助手，名为 MedLabAgent。请详细、完整地回答用户问题。");
        messages.addObject().put("role", "user").put("content", prompt);
        
        return objectMapper.writeValueAsString(root);
    }

    /**
     * 构建多轮对话请求体
     */
    private String buildRequestBodyWithMessages(List<Map<String, String>> chatMessages) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);
        root.put("stream", true);
        root.put("max_tokens", maxTokens);
        root.put("temperature", temperature);
        root.put("top_p", topP);

        ArrayNode messages = root.putArray("messages");
        for (Map<String, String> msg : chatMessages) {
            messages.addObject()
                    .put("role", msg.get("role"))
                    .put("content", msg.get("content"));
        }

        return objectMapper.writeValueAsString(root);
    }

    private String parseJsonContent(String jsonStr) {
        try {
            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                // 解析兼容模式下的 delta 结构
                JsonNode delta = choices.get(0).get("delta");
                if (delta != null && delta.has("content")) {
                    return delta.get("content").asText("");
                }
            }
        } catch (Exception e) {
            // 忽略非 JSON 数据
        }
        return "";
    }
}