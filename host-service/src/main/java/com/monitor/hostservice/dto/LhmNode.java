package com.monitor.hostservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LhmNode {

    private int id;

    @JsonProperty("Text")
    private String text;

    @JsonProperty("Min")
    private String min;

    @JsonProperty("Value")
    private String value;

    @JsonProperty("Max")
    private String max;

    @JsonProperty("HardwareId")
    private String hardwareId;

    @JsonProperty("SensorId")
    private String sensorId;

    @JsonProperty("Type")
    private String type;

    @JsonProperty("RawMin")
    private String rawMin;

    @JsonProperty("RawValue")
    private String rawValue;

    @JsonProperty("RawMax")
    private String rawMax;

    @JsonProperty("ImageURL")
    private String imageUrl;

    @JsonProperty("Children")
    private List<LhmNode> children;
}