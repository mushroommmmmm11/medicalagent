package com.medlab.controller;

import com.medlab.agent.AgentFuncRealize;
import com.medlab.entity.SessionMessage;
import com.medlab.service.BailianQianwenService;
import com.medlab.service.KnowledgeService;
import com.medlab.service.SessionChatService;
import com.medlab.service.StorageService;
import com.medlab.service.UserMedicalService;
import com.medlab.util.JwtTokenProvider;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AgentController {

    @Autowired
    private AgentFuncRealize agentFuncRealize;

    @Autowired
    private StorageService storageService;

    @Autowired
    private UserMedicalService userMedicalService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private BailianQianwenService bailianService;

    @Autowired
    private SessionChatService sessionChatService;

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
    public Flux<String> chatStream(@RequestParam String userQuery,
                                     @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 从 JWT 获取用户信息
        String userContext = "";
        UUID userId = null;
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                userId = jwtTokenProvider.getUserIdFromToken(token);
                userContext = userMedicalService.getMedicalSummaryForAI(userId);
            }
        } catch (Exception e) {
            // 获取用户档案失败不影响对话
        }

        // 读取该用户的历史对话
        List<SessionMessage> history = null;
        if (userId != null) {
            history = sessionChatService.getHistory(userId);
        }

        // 保存本次用户消息到会话数据库
        final UUID finalUserId = userId;
        if (finalUserId != null) {
            sessionChatService.saveMessage(finalUserId, "user", userQuery);
        }

        StringBuilder fullResponse = new StringBuilder();
        return agentFuncRealize.chatStream(userQuery, userContext, history)
                .map(token -> {
                    fullResponse.append(token);
                    try {
                        ObjectNode node = objectMapper.createObjectNode();
                        node.put("content", token);
                        return objectMapper.writeValueAsString(node);
                    } catch (Exception e) {
                        return "{\"content\":\"" + token.replace("\"", "\\\"") + "\"}";
                    }
                })
                .concatWith(Flux.defer(() -> {
                    // 保存AI回复到会话数据库
                    if (finalUserId != null) {
                        sessionChatService.saveMessage(finalUserId, "assistant", fullResponse.toString());
                    }
                    String metaEvent = parseMetaTag(fullResponse.toString());
                    return Flux.just(metaEvent, "[DONE]");
                }));
    }

    private String parseMetaTag(String content) {
        int metaStart = content.lastIndexOf("[META|");
        boolean isMedical = false;
        String diseases = "";
        String drugAllergies = "";
        if (metaStart != -1) {
            int metaEnd = content.indexOf("]", metaStart);
            if (metaEnd != -1) {
                String metaStr = content.substring(metaStart + 6, metaEnd);
                for (String part : metaStr.split("\\|")) {
                    part = part.trim();
                    if (part.startsWith("\u533b\u7597:") || part.startsWith("\u533b\u7597\uff1a")) {
                        isMedical = "\u662f".equals(part.substring(3).trim());
                    } else if (part.startsWith("\u75be\u75c5:") || part.startsWith("\u75be\u75c5\uff1a")) {
                        diseases = part.substring(3).trim();
                        if ("\u65e0".equals(diseases)) diseases = "";
                    } else if (part.startsWith("\u8fc7\u654f:") || part.startsWith("\u8fc7\u654f\uff1a")) {
                        drugAllergies = part.substring(3).trim();
                        if ("\u65e0".equals(drugAllergies)) drugAllergies = "";
                    }
                }
            }
        }
        try {
            ObjectNode meta = objectMapper.createObjectNode();
            meta.put("isMedical", isMedical);
            meta.put("diseases", diseases);
            meta.put("drugAllergies", drugAllergies);
            return "[META:" + objectMapper.writeValueAsString(meta) + "]";
        } catch (Exception e) {
            return "[META:{\"isMedical\":false,\"diseases\":\"\",\"drugAllergies\":\"\"}]";
        }
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
    public ResponseEntity<Map<String, String>> uploadReport(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            String filePath = storageService.saveFile(file);
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("filePath", filePath);
            response.put("fileName", file.getOriginalFilename());
            
            // 自动将文件信息追加到用户病历（无需确认）
            UUID userId = extractUserId(authHeader);
            if (userId != null) {
                String record = "上传报告:" + file.getOriginalFilename();
                userMedicalService.appendMedicalHistoryRaw(userId, record);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // ==================== 病历管理 API ====================
    
    /**
     * 追加病历记录（对话确认后调用）
     */
    @PostMapping("/user/medical-history/append")
    public ResponseEntity<Map<String, String>> appendMedicalHistory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String disease,
            @RequestParam String status) {
        try {
            UUID userId = extractUserId(authHeader);
            userMedicalService.appendMedicalHistory(userId, disease, status);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "病历记录已添加");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 更新过敏药物
     */
    @PostMapping("/user/drug-allergy/update")
    public ResponseEntity<Map<String, String>> updateDrugAllergy(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String drugAllergy) {
        try {
            UUID userId = extractUserId(authHeader);
            userMedicalService.updateDrugAllergy(userId, drugAllergy);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "过敏药物信息已更新");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 查询用户病历历史
     */
    @GetMapping("/user/medical-history")
    public ResponseEntity<Map<String, String>> getMedicalHistory(
            @RequestHeader("Authorization") String authHeader) {
        try {
            UUID userId = extractUserId(authHeader);
            String history = userMedicalService.getMedicalHistory(userId);
            String drugAllergy = userMedicalService.getDrugAllergy(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("medicalHistory", history != null ? history : "");
            response.put("drugAllergy", drugAllergy != null ? drugAllergy : "");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 从AI回复中提取疾病和药物过敏信息
     */
    @PostMapping("/agent/extract-keywords")
    public ResponseEntity<Map<String, Object>> extractKeywords(@RequestBody Map<String, String> request) {
        try {
            String text = request.get("text");
            if (text == null || text.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "text is required"));
            }
            if (text.length() > 800) text = text.substring(0, 800);
            String prompt = "判断以下对话是否涉及医疗诊断（疾病、症状、药物过敏、检验报告分析等），并提取关键信息。" +
                    "严格按以下格式返回三行，不要加任何解释：\n" +
                    "医疗:是\n" +
                    "疾病:急性扁桃体炎\n" +
                    "过敏:青霉素\n" +
                    "如果不是医疗诊断对话（如闲聊、天气、常识等），第一行写“医疗:否”，后两行写“无”。\n\n" + text;
            String result = bailianService.generateText(prompt);
            String diseases = "";
            String drugAllergies = "";
            boolean isMedical = false;
            if (result != null) {
                for (String line : result.trim().split("\\n")) {
                    line = line.trim();
                    if (line.startsWith("医疗:") || line.startsWith("医疗：")) {
                        String val = line.substring(3).trim();
                        isMedical = "是".equals(val);
                    } else if (line.startsWith("疾病:") || line.startsWith("疾病：")) {
                        diseases = line.substring(3).trim();
                        if ("无".equals(diseases)) diseases = "";
                    } else if (line.startsWith("过敏:") || line.startsWith("过敏：")) {
                        drugAllergies = line.substring(3).trim();
                        if ("无".equals(drugAllergies)) drugAllergies = "";
                    }
                }
            }
            Map<String, Object> response = new HashMap<>();
            response.put("isMedical", isMedical);
            response.put("diseases", diseases);
            response.put("drugAllergies", drugAllergies);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("isMedical", false, "diseases", "", "drugAllergies", ""));
        }
    }

    /**
     * 从 Authorization header 中提取用户ID
     */
    private UUID extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}