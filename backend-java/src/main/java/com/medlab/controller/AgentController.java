package com.medlab.controller;

import com.medlab.agent.AgentFuncRealize;
import com.medlab.service.BailianQianwenService;
import com.medlab.service.KnowledgeService;
import com.medlab.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AgentController {

    @Autowired
    private AgentFuncRealize agentFuncRealize;

    @Autowired
    private StorageService storageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "MedLabAgent System is running");
        return ResponseEntity.ok(response);
    }

    /**
     * 流式接口 - 用JSON包装每个token，确保换行符安全传输
     */
    @PostMapping(value = "/agent/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam String userQuery) {
        return agentFuncRealize.chatStream(userQuery)
                .map(token -> {
                    try {
                        ObjectNode node = objectMapper.createObjectNode();
                        node.put("content", token);
                        return objectMapper.writeValueAsString(node);
                    } catch (Exception e) {
                        return "{\"content\":\"" + token.replace("\"", "\\\"") + "\"}";
                    }
                });
    }

    /**
     * 同步对话接口
     */
    @PostMapping("/agent/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestParam String userQuery) {
        try {
            String aiResponse = agentFuncRealize.chat(userQuery);
            Map<String, String> result = new HashMap<>();
            result.put("status", "success");
            result.put("response", aiResponse);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/agent/upload-report")
    public ResponseEntity<Map<String, String>> uploadReport(@RequestParam("file") MultipartFile file) {
        try {
            String filePath = storageService.saveFile(file);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("filePath", filePath);
            response.put("fileName", file.getOriginalFilename());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}