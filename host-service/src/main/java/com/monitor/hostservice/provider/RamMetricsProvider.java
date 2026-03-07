package com.monitor.hostservice.provider;

import com.monitor.hostservice.dto.HardwareRecords.RamMetrics;
import com.monitor.hostservice.service.LhmDataManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;


@Component
@RequiredArgsConstructor
public class RamMetricsProvider {

    private final LhmDataManager lhmManager;
    private final GlobalMemory memory = new SystemInfo().getHardware().getMemory();

    public RamMetrics getMetrics() {
        long totalRam = memory.getTotal();
        long usedRam = totalRam - memory.getAvailable();

        return new RamMetrics(
                ((double) usedRam / totalRam) * 100,
                usedRam / 1073741824L,
                totalRam / 1073741824L,
                lhmManager.getRamTemp(),
                lhmManager.getRamPower()
        );
    }
}