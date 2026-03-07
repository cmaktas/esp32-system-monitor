package com.monitor.hostservice.provider;

import com.monitor.hostservice.dto.HardwareRecords.CpuMetrics;
import com.monitor.hostservice.service.LhmDataManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CpuMetricsProvider {
    private final LhmDataManager lhmManager;
    private final CentralProcessor processor = new SystemInfo().getHardware().getProcessor();
    private long[] prevTicks = processor.getSystemCpuLoadTicks();
    private long[][] prevCoreTicks = processor.getProcessorCpuLoadTicks();

    public CpuMetrics getMetrics() {
        double loadPercent = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        prevTicks = processor.getSystemCpuLoadTicks();

        double[] coreData = processor.getProcessorCpuLoadBetweenTicks(prevCoreTicks);
        prevCoreTicks = processor.getProcessorCpuLoadTicks();
        List<Double> coreLoads = Arrays.stream(coreData).mapToObj(v -> v * 100).toList();

        return new CpuMetrics(
                loadPercent,
                lhmManager.getCpuTemp(),
                lhmManager.getCpuPower(),
                (int) (Arrays.stream(processor.getCurrentFreq()).average().orElse(0) / 1_000_000),
                processor.getLogicalProcessorCount(),
                coreLoads
        );
    }
}