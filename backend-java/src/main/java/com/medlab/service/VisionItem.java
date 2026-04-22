package com.medlab.service;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VisionItem {
    private static final Pattern STRING_ITEM_PATTERN =
            Pattern.compile("^\\s*(.+?)\\s*:\\s*([-+]?[0-9]*\\.?[0-9]+)\\s*(.*)$");

    @JsonAlias({"name", "indicator", "test_name", "test_item"})
    private String item;
    private String value;
    private String unit;
    @JsonAlias({"reference_range", "range", "reference"})
    private String normal_range;
    @JsonAlias({"abnormal_flag", "result_flag"})
    private String status;

    public VisionItem() {
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static VisionItem fromString(String raw) {
        VisionItem item = new VisionItem();
        if (raw == null) {
            return item;
        }

        String trimmed = raw.trim();
        Matcher matcher = STRING_ITEM_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            item.setItem(matcher.group(1).trim());
            item.setValue(matcher.group(2).trim());
            item.setUnit(matcher.group(3).trim());
        } else {
            item.setItem(trimmed);
            item.setValue("");
            item.setUnit("");
        }
        item.setNormal_range("");
        item.setStatus("unknown");
        return item;
    }

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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getNormal_range() {
        return normal_range;
    }

    public void setNormal_range(String normal_range) {
        this.normal_range = normal_range;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
