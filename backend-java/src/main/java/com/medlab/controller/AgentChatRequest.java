package com.medlab.controller;

import com.medlab.service.AnalyzeVisionResponse;

public class AgentChatRequest {
    private String query;
    private String userId;
    private AnalyzeVisionResponse ocrResult;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public AnalyzeVisionResponse getOcrResult() {
        return ocrResult;
    }

    public void setOcrResult(AnalyzeVisionResponse ocrResult) {
        this.ocrResult = ocrResult;
    }
}
