package com.monitor.hostservice.service;

import com.monitor.hostservice.dto.SystemMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
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
    }

    public SystemMetrics getMetrics() {
        // --- CPU Load & Core Details ---
        double cpuLoadPercent = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = processor.getSystemCpuLoadTicks();

        double[] load = processor.getProcessorCpuLoadBetweenTicks(prevCoreTicks);
        prevCoreTicks = processor.getProcessorCpuLoadTicks();
        List<Double> coreLoads = new ArrayList<>();
        for (double v : load) {
            coreLoads.add(v * 100);
        }
        int coreCount = processor.getLogicalProcessorCount();

        // --- CPU Frequency (MHz) ---
        long[] freqs = processor.getCurrentFreq();
        int avgCpuFreqMhz = (int) (Arrays.stream(freqs).average().orElse(0) / 1_000_000);

        // --- RAM Calculation ---
        long totalRam = memory.getTotal();
        long availableRam = memory.getAvailable();
        long usedRam = totalRam - availableRam;

        double ramUsagePercent = ((double) usedRam / totalRam) * 100;
        long totalRamGb = totalRam / (1024 * 1024 * 1024);
        long usedRamGb = usedRam / (1024 * 1024 * 1024);

        // --- CPU Temperature & Power ---
        double cpuTemperature = sensors.getCpuTemperature();
        double cpuPowerWatts = getRealCpuPower();

        // --- GPU Metrics (including Temperature & Clock) ---
        double gpuLoadPercent = 0.0;
        long usedGpuVramGb = 0;
        long totalGpuVramGb = 0;
        double gpuPowerWatts = 0.0;
        int gpuClockMhz = 0;
        double gpuTemperature = 0.0;

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "nvidia-smi",
                    "--query-gpu=utilization.gpu,memory.used,memory.total,power.draw,clocks.current.graphics,temperature.gpu",
                    "--format=csv,noheader,nounits"
            );
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            if (line != null && !line.isBlank()) {
                String[] parts = line.split(",");
                if (parts.length == 6) {
                    gpuLoadPercent = Double.parseDouble(parts[0].trim());


                    double usedMb = Double.parseDouble(parts[1].trim());
                    double totalMb = Double.parseDouble(parts[2].trim());

                    usedGpuVramGb = (long) Math.ceil(usedMb / 1024.0);
                    totalGpuVramGb = (long) Math.round(totalMb / 1024.0);

                    gpuPowerWatts = Double.parseDouble(parts[3].trim());
                    gpuClockMhz = Integer.parseInt(parts[4].trim());
                    gpuTemperature = Double.parseDouble(parts[5].trim());
                }
            }
        } catch (Exception e) {
            log.trace("GPU metrics failed.");
        }

        return new SystemMetrics(
                cpuLoadPercent,
                ramUsagePercent,
                totalRamGb,
                usedRamGb,
                cpuTemperature,
                gpuTemperature,
                coreCount,
                coreLoads,
                gpuLoadPercent,
                usedGpuVramGb,
                totalGpuVramGb,
                cpuPowerWatts,
                gpuPowerWatts,
                avgCpuFreqMhz,
                gpuClockMhz
        );
    }

    private double getRealCpuPower() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "powershell",
                    "-Command",
                    "(Get-Counter '\\Processor Information(_Total)\\Processor Power').CounterSamples.CookedValue"
            );
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            if (line != null && !line.isBlank()) {
                return Double.parseDouble(line.trim());
            }
        } catch (Exception e) {
            log.trace("Real CPU power reading failed, using load-based fallback.");
        }
        return (processor.getSystemCpuLoadBetweenTicks(prevTicks) * 125.0) + 10.0;
    }
}