package com.medlab.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabReportInsightServiceTest {

    @Mock
    private OcrServiceClient ocrServiceClient;

    @InjectMocks
    private LabReportInsightService labReportInsightService;

    @Test
    void buildInsights_shouldIncludeOcrResultDeviationAndSortedAbnormalItems() {
        AnalyzeVisionResponse ocrResponse = new AnalyzeVisionResponse();
        ocrResponse.setStatus("success");

        VisionItem wbc = visionItem("WBC", "11.4", "x10^9/L", "3.5-9.5", "high");
        VisionItem ly = visionItem("LY%", "16.1", "%", "20-50", "low");
        VisionItem rbc = visionItem("RBC", "4.56", "x10^12/L", "3.8-5.1", "normal");
        ocrResponse.setAnalysis(List.of(wbc, ly, rbc));

        when(ocrServiceClient.analyzeVisionAsync("http://example.test/report.png", null))
                .thenReturn(Mono.just(ocrResponse));

        LabReportInsightsResponse response =
                labReportInsightService.buildInsights("http://example.test/report.png");

        assertEquals(3, response.getTotalItems());
        assertEquals(2, response.getAbnormalCount());
        assertSame(ocrResponse, response.getOcrResult());
        assertEquals(2, response.getAbnormalItems().size());
        assertEquals("WBC", response.getAbnormalItems().get(0).getItem());
        assertEquals("LY%", response.getAbnormalItems().get(1).getItem());
        assertEquals("偏高", response.getAbnormalItems().get(0).getStatus());
        assertEquals("偏低", response.getAbnormalItems().get(1).getStatus());
        assertEquals("0.422", response.getAbnormalItems().get(0).getDeviationLabel());
        assertEquals("0.130", response.getAbnormalItems().get(1).getDeviationLabel());
        assertFalse(response.getTrendView().isAvailable());
        assertNotNull(response.getTrendView().getMessage());
    }

    @Test
    void buildInsights_shouldAcceptLegacyStringAnalysisItems() {
        AnalyzeVisionResponse ocrResponse = new AnalyzeVisionResponse();
        ocrResponse.setStatus("success");
        ocrResponse.setAnalysis(List.of(
                VisionItem.fromString("C反应蛋白(超敏)CRP: 18.11 mg/L"),
                VisionItem.fromString("白细胞计数WBC: 11.42 x10^9/L")
        ));

        when(ocrServiceClient.analyzeVisionAsync("http://example.test/legacy-report.png", null))
                .thenReturn(Mono.just(ocrResponse));

        LabReportInsightsResponse response =
                labReportInsightService.buildInsights("http://example.test/legacy-report.png");

        assertEquals(2, response.getTotalItems());
        assertEquals("C反应蛋白(超敏)CRP", response.getItems().get(0).getItem());
        assertEquals("18.11", response.getItems().get(0).getValue());
        assertEquals("mg/L", response.getItems().get(0).getUnit());
    }

    private VisionItem visionItem(String item, String value, String unit, String normalRange, String status) {
        VisionItem visionItem = new VisionItem();
        visionItem.setItem(item);
        visionItem.setValue(value);
        visionItem.setUnit(unit);
        visionItem.setNormal_range(normalRange);
        visionItem.setStatus(status);
        return visionItem;
    }
}
