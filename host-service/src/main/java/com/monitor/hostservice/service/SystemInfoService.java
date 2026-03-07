package com.monitor.hostservice.service;

import com.monitor.hostservice.dto.SystemMetrics;
import com.monitor.hostservice.provider.CpuMetricsProvider;
import com.monitor.hostservice.provider.GpuMetricsProvider;
import com.monitor.hostservice.provider.RamMetricsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemInfoService {
    private final LhmDataManager lhmManager;
    private final CpuMetricsProvider cpuProvider;
    private final GpuMetricsProvider gpuProvider;
    private final RamMetricsProvider ramProvider;

    public SystemMetrics getMetrics() {
        lhmManager.refresh();

        var cpu = cpuProvider.getMetrics();
        var gpu = gpuProvider.getMetrics();
        var ram = ramProvider.getMetrics();

        return new SystemMetrics(
                cpu.load(), cpu.temp(), cpu.power(), cpu.freq(), cpu.cores(), cpu.coreLoads(),
                gpu.load(), gpu.temp(), gpu.power(), gpu.clock(), gpu.usedVram(), gpu.totalVram(),
                ram.usage(), ram.usedGb(), ram.totalGb(), ram.temp(), ram.power()
        );
    }
}