package com.monitor.hostservice.service;

import com.monitor.hostservice.dto.SystemMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;
import oshi.hardware.GraphicsCard;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class SystemInfoService {

    private final HardwareAbstractionLayer hal;
    private final CentralProcessor processor;
    private final GlobalMemory memory;
    private final Sensors sensors;
    private long[] prevTicks;
    private long[][] prevCoreTicks;

    public SystemInfoService() {
        SystemInfo systemInfo = new SystemInfo();
        this.hal = systemInfo.getHardware();
        this.processor = hal.getProcessor();
        this.memory = hal.getMemory();
        this.sensors = hal.getSensors();

        this.prevTicks = processor.getSystemCpuLoadTicks();
        this.prevCoreTicks = processor.getProcessorCpuLoadTicks();

        log.info("SystemInfoService initialized. OS Family: {}", systemInfo.getOperatingSystem().getFamily());
    }

    public SystemMetrics getMetrics() {
        double cpuLoadPercent = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = processor.getSystemCpuLoadTicks();

        double[] load = processor.getProcessorCpuLoadBetweenTicks(prevCoreTicks);
        prevCoreTicks = processor.getProcessorCpuLoadTicks();
        List<Double> coreLoads = new ArrayList<>();
        for (double v : load) {
            coreLoads.add(v * 100);
        }
        int coreCount = processor.getLogicalProcessorCount();

        long totalRam = memory.getTotal();
        long availableRam = memory.getAvailable();
        long usedRam = totalRam - availableRam;

        double ramUsagePercent = ((double) usedRam / totalRam) * 100;
        long totalRamGb = totalRam / (1024 * 1024 * 1024);
        long usedRamGb = usedRam / (1024 * 1024 * 1024);

        double cpuTemperature = sensors.getCpuTemperature();

        List<GraphicsCard> graphicsCards = hal.getGraphicsCards();
        String gpuName = "Unknown GPU";
        long gpuVramGb = 0;
        if (!graphicsCards.isEmpty()) {
            GraphicsCard gpu = graphicsCards.get(0);
            gpuName = gpu.getName();
            gpuVramGb = gpu.getVRam() / (1024 * 1024 * 1024);
        }

        return new SystemMetrics(
                cpuLoadPercent,
                ramUsagePercent,
                totalRamGb,
                usedRamGb,
                cpuTemperature,
                coreCount,
                coreLoads,
                gpuName,
                gpuVramGb
        );
    }
}