package com.monitor.hostservice.dto;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Locale;

public record SystemMetrics(
        // CPU
        double cpuLoadPercent,
        double cpuTemperature,
        double cpuPowerWatts,
        int cpuFrequencyMhz,
        int coreCount,
        List<Double> coreLoads,

        // GPU
        double gpuLoadPercent,
        double gpuTemperature,
        double gpuPowerWatts,
        int gpuClockMhz,
        long usedGpuVramGigabytes,
        long gpuVramGigabytes,

        // RAM
        double ramUsagePercent,
        long usedRamGigabytes,
        long totalRamGigabytes,
        double ramTemperature,
        double ramPowerWatts
) {
    public String toSerialFormat() {
        String coresStr = coreLoads.stream()
                .map(load -> String.format(Locale.US, "%.1f", load))
                .collect(Collectors.joining(","));

        return String.format(Locale.US,
                "C:%.1f|R:%.1f|TR:%d|UR:%d|T:%.1f|GT:%.1f|N:%d|GV:%d|GL:%.1f|GU:%d|CP:%.1f|GP:%.1f|CF:%d|GF:%d|RT:%.1f|RP:%.1f|CL:%s%n",
                cpuLoadPercent, ramUsagePercent, totalRamGigabytes, usedRamGigabytes,
                cpuTemperature, gpuTemperature, coreCount, gpuVramGigabytes, gpuLoadPercent,
                usedGpuVramGigabytes, cpuPowerWatts, gpuPowerWatts,
                cpuFrequencyMhz, gpuClockMhz,
                ramTemperature, ramPowerWatts, // Yeni eklenenler
                coresStr);
    }
}