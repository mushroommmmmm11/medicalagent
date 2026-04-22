package com.medlab.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OcrServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OcrServiceClient.class);

    @Qualifier("pythonOcrWebClient")
    private final WebClient pythonOcrWebClient;

    public Mono<AnalyzeVisionResponse> analyzeVisionAsync(String filePath, String modelName) {
        return analyzeVisionAsync(filePath, modelName, false, null);
    }

    public Mono<AnalyzeVisionResponse> recheckVisionAsync(String filePath, String modelName, String focusItem) {
        return analyzeVisionAsync(filePath, modelName, true, focusItem);
    }

    public Mono<AnalyzeVisionResponse> analyzeVisionAsync(
            String filePath,
            String modelName,
            boolean forceRecheck,
            String focusItem
    ) {
        AnalyzeVisionRequest request = new AnalyzeVisionRequest(
                filePath,
                modelName,
                forceRecheck,
                focusItem
        );

        log.info("Calling OCR service: file={}, forceRecheck={}, focusItem={}", filePath, forceRecheck, focusItem);

        return pythonOcrWebClient
                .post()
                .uri("/api/v1/analyze-vision")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new OcrServiceException(
                                        "Python OCR service error: " + errorBody,
                                        clientResponse.statusCode().value()
                                )))
                )
                .bodyToMono(AnalyzeVisionResponse.class)
                .doOnSuccess(response -> {
                    int size = response.getAnalysis() == null ? 0 : response.getAnalysis().size();
                    log.info("OCR response received: file={}, cached={}, items={}",
                            filePath, response.getCached(), size);
                })
                .onErrorResume(error -> {
                    if (error instanceof OcrServiceException) {
                        return Mono.error(error);
                    }
                    log.error("Failed to call OCR service: {}", error.getMessage(), error);
                    return Mono.error(new OcrServiceException(
                            "Failed to call Python OCR service: " + error.getMessage(),
                            500
                    ));
                });
    }

    public Mono<Void> prefetchVisionCache(String filePath, String modelName) {
        return analyzeVisionAsync(filePath, modelName, false, null)
                .then()
                .doOnSuccess(unused -> log.info("OCR prefetch completed for {}", filePath))
                .doOnError(error -> log.warn("OCR prefetch failed for {}: {}", filePath, error.getMessage()));
    }

    public Mono<Boolean> healthCheckAsync() {
        return pythonOcrWebClient
                .get()
                .uri("/health")
                .retrieve()
                .toEntity(Map.class)
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorResume(error -> {
                    log.error("Python OCR service is unavailable: {}", error.getMessage());
                    return Mono.just(false);
                });
    }

    public Mono<Map<String, AnalyzeVisionResponse>> analyzeMultipleFilesAsync(List<String> filePaths) {
        return Mono.just(filePaths)
                .flatMapMany(paths -> reactor.core.publisher.Flux.fromIterable(paths))
                .flatMap(filePath -> analyzeVisionAsync(filePath, null)
                        .map(response -> Map.entry(filePath, response))
                        .onErrorResume(error -> Mono.just(Map.entry(
                                filePath,
                                AnalyzeVisionResponse.errorResponse("Failed: " + error.getMessage())
                        ))))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    static class AnalyzeVisionRequest {
        private String path;
        private String model;
        private boolean force_recheck;
        private String focus_item;

        public AnalyzeVisionRequest() {
        }

        public AnalyzeVisionRequest(String path, String model, boolean force_recheck, String focus_item) {
            this.path = path;
            this.model = model;
            this.force_recheck = force_recheck;
            this.focus_item = focus_item;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public boolean isForce_recheck() {
            return force_recheck;
        }

        public void setForce_recheck(boolean force_recheck) {
            this.force_recheck = force_recheck;
        }

        public String getFocus_item() {
            return focus_item;
        }

        public void setFocus_item(String focus_item) {
            this.focus_item = focus_item;
        }
    }
}
