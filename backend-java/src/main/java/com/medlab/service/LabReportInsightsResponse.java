package com.medlab.service;

import java.util.ArrayList;
import java.util.List;

public class LabReportInsightsResponse {
    private String filePath;
    private int totalItems;
    private int abnormalCount;
    private String generatedAt;
    private AnalyzeVisionResponse ocrResult;
    private List<LabItemInsight> abnormalItems = new ArrayList<>();
    private List<LabItemInsight> items = new ArrayList<>();
    private LabTrendView trendView = new LabTrendView();

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getAbnormalCount() {
        return abnormalCount;
    }

    public void setAbnormalCount(int abnormalCount) {
        this.abnormalCount = abnormalCount;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public AnalyzeVisionResponse getOcrResult() {
        return ocrResult;
    }

    public void setOcrResult(AnalyzeVisionResponse ocrResult) {
        this.ocrResult = ocrResult;
    }

    public List<LabItemInsight> getAbnormalItems() {
        return abnormalItems;
    }

    public void setAbnormalItems(List<LabItemInsight> abnormalItems) {
        this.abnormalItems = abnormalItems;
    }

    public List<LabItemInsight> getItems() {
        return items;
    }

    public void setItems(List<LabItemInsight> items) {
        this.items = items;
    }

    public LabTrendView getTrendView() {
        return trendView;
    }

    public void setTrendView(LabTrendView trendView) {
        this.trendView = trendView;
    }
}
