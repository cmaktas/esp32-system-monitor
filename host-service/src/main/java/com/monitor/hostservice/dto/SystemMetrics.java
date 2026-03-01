package com.monitor.hostservice.dto;

import java.util.List;
import java.util.stream.Collectors;

public record SystemMetrics(
        double cpuLoadPercent,
        double ramUsagePercent,
        long totalRamGigabytes,
        long usedRamGigabytes,
        double cpuTemperature,
        int coreCount,
        List<Double> coreLoads,
        String gpuName,
        long gpuVramGigabytes
) {
    public String toSerialFormat() {
        // Çekirdek yükleri listesini virgülle ayrılmış bir string'e çeviriyoruz (Örn: "15.2,0.5,45.0")
        String coresStr = coreLoads.stream()
                .map(load -> String.format(java.util.Locale.US, "%.1f", load))
                .collect(Collectors.joining(","));

        // GPU adında olası null veya pipe (|) karakteri sorunlarını önleyelim
        String safeGpuName = (gpuName != null && !gpuName.isBlank())
                ? gpuName.replace("|", "")
                : "Unknown GPU";

        // Format: C:CPU%|R:RAM%|TR:TotalRAM|UR:UsedRAM|T:Temp|N:CoreCount|GN:GpuName|GV:GpuVram|CL:Core1,Core2...
        return String.format(java.util.Locale.US,
                "C:%.1f|R:%.1f|TR:%d|UR:%d|T:%.1f|N:%d|GN:%s|GV:%d|CL:%s%n",
                cpuLoadPercent,
                ramUsagePercent,
                totalRamGigabytes,
                usedRamGigabytes,
                cpuTemperature,
                coreCount,
                safeGpuName,
                gpuVramGigabytes,
                coresStr);
    }
}