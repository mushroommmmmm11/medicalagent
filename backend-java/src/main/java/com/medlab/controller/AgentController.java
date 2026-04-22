package com.medlab.controller;

import com.medlab.service.AnalyzeVisionResponse;
import com.medlab.service.LabReportInsightService;
import com.medlab.service.LabReportInsightsResponse;
import com.medlab.service.MedicalFacadeService;
import com.medlab.service.SessionChatService;
import com.medlab.service.UserMedicalService;
import com.medlab.util.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AgentController {

    private static final Logger logger = LoggerFactory.getLogger(AgentController.class);
    private static final Pattern REPORT_FILE_URL_PATTERN = Pattern.compile(
            "https?://[^\\s]+/api/v1/file/view/[^\\s]+",
            Pattern.CASE_INSENSITIVE
    );

    @Value("${langchain.service.url:http://localhost:8000}")
    private String langchainServiceUrl;

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private UserMedicalService userMedicalService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SessionChatService sessionChatService;

    @Autowired
    private MedicalFacadeService medicalFacadeService;

    @Autowired
    private LabReportInsightService labReportInsightService;

    private WebClient getWebClient() {
        return webClientBuilder.baseUrl(langchainServiceUrl).build();
    }

    @PostMapping(value = "/agent/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam(required = false) String userQuery,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) AgentChatRequest request
    ) {
        SseEmitter emitter = new SseEmitter(0L);

        String queryText = firstNonBlank(userQuery, request != null ? request.getQuery() : null);
        if (queryText == null) {
            emitter.completeWithError(new IllegalArgumentException("userQuery is required"));
            return emitter;
        }

        String currentUserId = resolveUserId(authHeader, request != null ? request.getUserId() : null);
        AnalyzeVisionResponse ocrResult = request != null ? request.getOcrResult() : null;
        if (ocrResult == null) {
            String filePath = extractReportFilePath(queryText);
            if (filePath != null) {
                try {
                    ocrResult = labReportInsightService.fetchOcrResponse(filePath);
                    logger.info("Hydrated OCR result for stream chat from report URL: {}", filePath);
                } catch (Exception ex) {
                    logger.warn("Failed to prefetch OCR result for stream chat: {}", ex.getMessage());
                }
            }
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", queryText);
        if (currentUserId != null) {
            requestBody.put("user_id", currentUserId);
        }
        if (ocrResult != null) {
            requestBody.put("ocr_result", ocrResult);
        }

        final long streamStartedAt = System.nanoTime();
        final AtomicLong lastChunkAt = new AtomicLong(streamStartedAt);
        final AtomicInteger chunkCounter = new AtomicInteger(0);

        try {
            WebClient client = getWebClient();
            client.post()
                    .uri("/api/v1/agent/chat/stream")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", authHeader != null ? authHeader : "")
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .subscribe(
                            chunk -> {
                                try {
                                    long now = System.nanoTime();
                                    int chunkIndex = chunkCounter.incrementAndGet();
                                    double deltaMs = (now - lastChunkAt.getAndSet(now)) / 1_000_000.0;
                                    double totalMs = (now - streamStartedAt) / 1_000_000.0;
                                    String preview = chunk.length() > 120 ? chunk.substring(0, 120) + "..." : chunk;
                                    logger.info(
                                            "Streaming passthrough chunk #{} len={} deltaMs={} totalMs={} preview={}",
                                            chunkIndex,
                                            chunk.length(),
                                            String.format("%.1f", deltaMs),
                                            String.format("%.1f", totalMs),
                                            preview.replaceAll("\\s+", " ")
                                    );
                                    emitter.send(chunk);
                                } catch (Exception ex) {
                                    emitter.completeWithError(ex);
                                }
                            },
                            err -> {
                                logger.error("Forwarding Python stream failed", err);
                                emitter.completeWithError(err);
                            },
                            () -> {
                                double totalMs = (System.nanoTime() - streamStartedAt) / 1_000_000.0;
                                logger.info(
                                        "Streaming passthrough completed chunkCount={} totalMs={}",
                                        chunkCounter.get(),
                                        String.format("%.1f", totalMs)
                                );
                                emitter.complete();
                            }
                    );
        } catch (Exception ex) {
            logger.error("Failed to initialize stream proxy", ex);
            emitter.completeWithError(ex);
        }

        return emitter;
    }

    @GetMapping("/internal/user/medical-history")
    public ResponseEntity<Map<String, String>> getMedicalHistoryById(@RequestParam("userId") String userId) {
        Map<String, String> resp = new HashMap<>();
        UUID uid;
        try {
            uid = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            resp.put("status", "error");
            resp.put("message", "无效的 UUID 格式");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
        }

        try {
            String history = userMedicalService.getMedicalHistory(uid);
            String drug = userMedicalService.getDrugAllergy(uid);

            resp.put("status", "success");
            resp.put("medicalHistory", history != null ? history : "暂无病历记录");
            resp.put("drugAllergy", drug != null ? drug : "无已知过敏史");
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            logger.warn("Querying medical history failed, falling back to empty default. userId={}", userId, e);
            resp.put("status", "success");
            resp.put("medicalHistory", "暂无病历记录");
            resp.put("drugAllergy", "无已知过敏史");
            return ResponseEntity.ok(resp);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "MedLabAgent Backend is running");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/agent/analyze")
    public ResponseEntity<Map<String, String>> analyzeReport(
            @RequestParam String reportContent,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        return ResponseEntity.ok(sessionChatService.analyzeWithAuth(authHeader, reportContent));
    }

    @PostMapping("/agent/upload-report")
    public ResponseEntity<Map<String, String>> uploadReport(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        return ResponseEntity.ok(medicalFacadeService.handleUploadAndAppend(file, authHeader));
    }

    @GetMapping("/agent/report-insights")
    public ResponseEntity<LabReportInsightsResponse> getReportInsights(@RequestParam String filePath) {
        return ResponseEntity.ok(labReportInsightService.buildInsights(filePath));
    }

    private String resolveUserId(String authHeader, String fallbackUserId) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                if (jwtTokenProvider.validateToken(token)) {
                    String userId = jwtTokenProvider.getUserIdFromToken(token).toString();
                    logger.info("Resolved authenticated user ID: {}", userId);
                    return userId;
                }
            } catch (Exception e) {
                logger.warn("Failed to parse token, fallback to request userId: {}", e.getMessage());
            }
        }
        return firstNonBlank(fallbackUserId);
    }

    private String extractReportFilePath(String queryText) {
        Matcher matcher = REPORT_FILE_URL_PATTERN.matcher(queryText);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group().replaceAll("[,，。；;]+$", "");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
