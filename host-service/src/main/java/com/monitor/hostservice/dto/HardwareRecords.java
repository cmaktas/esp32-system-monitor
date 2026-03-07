package com.monitor.hostservice.dto;

import java.util.List;

public class HardwareRecords {
    public record CpuMetrics(double load, double temp, double power, int freq, int cores, List<Double> coreLoads) {
    }

    public record GpuMetrics(double load, double temp, double power, int clock, long usedVram, long totalVram) {
    }

    public record RamMetrics(double usage, long usedGb, long totalGb, double temp, double power) {
    }
}