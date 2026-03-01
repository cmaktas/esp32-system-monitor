package com.monitor.hostservice.service;

import com.monitor.hostservice.dto.SystemMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

@Slf4j
@Service
public class SystemInfoService {

    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private long[] prevTicks;

    public SystemInfoService() {
        SystemInfo systemInfo = new SystemInfo();
        HardwareAbstractionLayer hal = systemInfo.getHardware();
        this.processor = hal.getProcessor();
        this.memory = hal.getMemory();
        this.prevTicks = processor.getSystemCpuLoadTicks();

        log.info("SystemInfoService initialized. OS Family: {}", systemInfo.getOperatingSystem().getFamily());
    }

    public SystemMetrics getMetrics() {
        double cpuLoadPercent = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = processor.getSystemCpuLoadTicks();

        long totalRam = memory.getTotal();
        long availableRam = memory.getAvailable();
        long usedRam = totalRam - availableRam;

        double ramUsagePercent = ((double) usedRam / totalRam) * 100;

        long totalRamGb = totalRam / (1024 * 1024 * 1024);
        long usedRamGb = usedRam / (1024 * 1024 * 1024);

        return new SystemMetrics(cpuLoadPercent, ramUsagePercent, totalRamGb, usedRamGb);
    }
}