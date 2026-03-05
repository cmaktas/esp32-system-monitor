package com.monitor.hostservice.dto;

import java.util.List;
import java.util.stream.Collectors;

public record SystemMetrics(
        double cpuLoadPercent,
        double ramUsagePercent,
        long totalRamGigabytes,
        long usedRamGigabytes,
        double cpuTemperature,   // T:
        double gpuTemperature,   // GT:
        int coreCount,           // N:
        List<Double> coreLoads,  // CL:
        double gpuLoadPercent,   // GL:
        long usedGpuVramGigabytes, // GU:
        long gpuVramGigabytes,   // GV:
        double cpuPowerWatts,    // CP:
        double gpuPowerWatts,    // GP:
        int cpuFrequencyMhz,     // CF:
        int gpuClockMhz          // GF:
) {
    public String toSerialFormat() {
        String coresStr = coreLoads.stream()
                .map(load -> String.format(java.util.Locale.US, "%.1f", load))
                .collect(Collectors.joining(","));

        return String.format(java.util.Locale.US,
                "C:%.1f|R:%.1f|TR:%d|UR:%d|T:%.1f|GT:%.1f|N:%d|GV:%d|GL:%.1f|GU:%d|CP:%.1f|GP:%.1f|CF:%d|GF:%d|CL:%s%n",
                cpuLoadPercent, ramUsagePercent, totalRamGigabytes, usedRamGigabytes,
                cpuTemperature, gpuTemperature, coreCount, gpuVramGigabytes, gpuLoadPercent,
                usedGpuVramGigabytes, cpuPowerWatts, gpuPowerWatts,
                cpuFrequencyMhz, gpuClockMhz, coresStr);
    }
}