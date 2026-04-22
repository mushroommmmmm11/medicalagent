package com.medlab.service;

public class LabItemInsight {
    private String item;
    private String value;
    private Double numericValue;
    private String unit;
    private String normalRange;
    private String status;
    private String direction;
    private boolean abnormal;
    private Double lln;
    private Double uln;
    private Double deviationScore;
    private String deviationLabel;

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Double getNumericValue() {
        return numericValue;
    }

    public void setNumericValue(Double numericValue) {
        this.numericValue = numericValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getNormalRange() {
        return normalRange;
    }

    public void setNormalRange(String normalRange) {
        this.normalRange = normalRange;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public boolean isAbnormal() {
        return abnormal;
    }

    public void setAbnormal(boolean abnormal) {
        this.abnormal = abnormal;
    }

    public Double getLln() {
        return lln;
    }

    public void setLln(Double lln) {
        this.lln = lln;
    }

    public Double getUln() {
        return uln;
    }

    public void setUln(Double uln) {
        this.uln = uln;
    }

    public Double getDeviationScore() {
        return deviationScore;
    }

    public void setDeviationScore(Double deviationScore) {
        this.deviationScore = deviationScore;
    }

    public String getDeviationLabel() {
        return deviationLabel;
    }

    public void setDeviationLabel(String deviationLabel) {
        this.deviationLabel = deviationLabel;
    }
}
