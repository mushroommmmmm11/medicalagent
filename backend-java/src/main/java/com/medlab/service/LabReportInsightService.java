package com.medlab.service;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LabReportInsightService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+");

    private final OcrServiceClient ocrServiceClient;

    public LabReportInsightService(OcrServiceClient ocrServiceClient) {
        this.ocrServiceClient = ocrServiceClient;
    }

    public AnalyzeVisionResponse fetchOcrResponse(String filePath) {
        AnalyzeVisionResponse ocrResponse = ocrServiceClient.analyzeVisionAsync(filePath, null).block();
        if (ocrResponse == null) {
            throw new IllegalStateException("OCR response is empty");
        }
        return ocrResponse;
    }

    public LabReportInsightsResponse buildInsights(String filePath) {
        AnalyzeVisionResponse ocrResponse = fetchOcrResponse(filePath);

        List<LabItemInsight> items = new ArrayList<>();
        List<LabItemInsight> abnormalItems = new ArrayList<>();

        if (ocrResponse.getAnalysis() != null) {
            for (VisionItem visionItem : ocrResponse.getAnalysis()) {
                LabItemInsight insight = toInsight(visionItem);
                items.add(insight);
                if (insight.isAbnormal()) {
                    abnormalItems.add(insight);
                }
            }
        }

        abnormalItems.sort(
                Comparator.comparing(
                        item -> item.getDeviationScore() == null ? -1.0 : item.getDeviationScore(),
                        Comparator.reverseOrder()
                )
        );

        LabTrendView trendView = new LabTrendView();
        trendView.setAvailable(false);
        trendView.setReportCount(1);
        trendView.setMessage("当前仅识别到 1 次化验单，继续上传历史化验后可展示趋势视图。");

        LabReportInsightsResponse response = new LabReportInsightsResponse();
        response.setFilePath(filePath);
        response.setGeneratedAt(OffsetDateTime.now().toString());
        response.setTotalItems(items.size());
        response.setAbnormalCount(abnormalItems.size());
        response.setOcrResult(ocrResponse);
        response.setItems(items);
        response.setAbnormalItems(abnormalItems);
        response.setTrendView(trendView);
        return response;
    }

    private LabItemInsight toInsight(VisionItem item) {
        LabItemInsight insight = new LabItemInsight();
        insight.setItem(item.getItem());
        insight.setValue(item.getValue());
        insight.setUnit(item.getUnit());
        insight.setNormalRange(item.getNormal_range());

        Double numericValue = parseDouble(item.getValue());
        insight.setNumericValue(numericValue);

        RangeBounds bounds = parseRange(item.getNormal_range());
        insight.setLln(bounds.lower());
        insight.setUln(bounds.upper());

        String direction = resolveDirection(item.getStatus(), numericValue, bounds);
        insight.setDirection(direction);
        insight.setAbnormal(!"normal".equals(direction));
        insight.setStatus(renderStatus(direction, item.getStatus()));

        if (!"normal".equals(direction) && numericValue != null) {
            Double deviation = computeDeviation(direction, numericValue, bounds);
            insight.setDeviationScore(deviation);
            insight.setDeviationLabel(formatDeviation(deviation));
        }

        return insight;
    }

    private String resolveDirection(String rawStatus, Double value, RangeBounds bounds) {
        String status = (rawStatus == null ? "" : rawStatus).trim().toLowerCase(Locale.ROOT);
        if (status.contains("高") || "high".equals(status) || "h".equals(status)) {
            return "high";
        }
        if (status.contains("低") || "low".equals(status) || "l".equals(status)) {
            return "low";
        }
        if (status.contains("正常") || "normal".equals(status) || "n".equals(status)) {
            return "normal";
        }
        if (value != null && bounds.lower() != null && value < bounds.lower()) {
            return "low";
        }
        if (value != null && bounds.upper() != null && value > bounds.upper()) {
            return "high";
        }
        return "normal";
    }

    private String renderStatus(String direction, String rawStatus) {
        if ("high".equals(direction)) {
            return "偏高";
        }
        if ("low".equals(direction)) {
            return "偏低";
        }
        if ("normal".equals(direction)) {
            return "正常";
        }
        return rawStatus == null || rawStatus.isBlank() ? "未知" : rawStatus;
    }

    private Double computeDeviation(String direction, double value, RangeBounds bounds) {
        if ("low".equals(direction) && bounds.lower() != null) {
            double denominator = bounds.hasSpan() ? bounds.span() : safeDenominator(bounds.lower());
            return round3((bounds.lower() - value) / denominator);
        }
        if ("high".equals(direction) && bounds.upper() != null) {
            double denominator = bounds.hasSpan() ? bounds.span() : safeDenominator(bounds.upper());
            return round3((value - bounds.upper()) / denominator);
        }
        return null;
    }

    private double safeDenominator(double value) {
        return Math.abs(value) < 1e-9 ? 1.0 : Math.abs(value);
    }

    private String formatDeviation(Double deviation) {
        if (deviation == null) {
            return "";
        }
        return String.format(Locale.ROOT, "%.3f", deviation);
    }

    private Double round3(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 1000.0) / 1000.0;
    }

    private Double parseDouble(String raw) {
        if (raw == null) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Double.parseDouble(matcher.group());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private RangeBounds parseRange(String rawRange) {
        if (rawRange == null || rawRange.isBlank()) {
            return new RangeBounds(null, null);
        }
        List<Double> numbers = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(rawRange.replace("~", "-"));
        while (matcher.find()) {
            try {
                numbers.add(Double.parseDouble(matcher.group()));
            } catch (NumberFormatException ignored) {
            }
        }
        if (numbers.size() >= 2) {
            return new RangeBounds(numbers.get(0), numbers.get(1));
        }
        String normalized = rawRange.replace("≤", "<=").replace("≥", ">=");
        if (numbers.size() == 1) {
            if (normalized.contains("<")) {
                return new RangeBounds(null, numbers.get(0));
            }
            if (normalized.contains(">")) {
                return new RangeBounds(numbers.get(0), null);
            }
        }
        return new RangeBounds(null, null);
    }

    private record RangeBounds(Double lower, Double upper) {
        boolean hasSpan() {
            return lower != null && upper != null && upper > lower;
        }

        double span() {
            return upper - lower;
        }
    }
}
