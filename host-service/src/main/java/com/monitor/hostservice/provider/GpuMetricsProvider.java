package com.monitor.hostservice.provider;

import com.monitor.hostservice.dto.HardwareRecords.GpuMetrics;
import com.monitor.hostservice.service.LhmDataManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GpuMetricsProvider {
    private final LhmDataManager lhmManager;

    public GpuMetrics getMetrics() {
        return new GpuMetrics(
                lhmManager.getGpuLoad(),
                lhmManager.getGpuTemp(),
                lhmManager.getGpuPower(),
                lhmManager.getGpuClock(),
                lhmManager.getGpuUsedVram(),
                lhmManager.getGpuTotalVram()
        );
    }
}