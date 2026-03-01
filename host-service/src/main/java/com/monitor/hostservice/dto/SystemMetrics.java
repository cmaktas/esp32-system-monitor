package com.monitor.hostservice.dto;

public record SystemMetrics(
        double cpuLoadPercent,
        double ramUsagePercent,
        long totalRamGigabytes,
        long usedRamGigabytes
) {
    public String toSerialFormat() {
        return String.format(java.util.Locale.US, "C:%.1f|R:%.1f%n", cpuLoadPercent, ramUsagePercent);
    }
}