package com.monitor.hostservice.scheduler;

import com.monitor.hostservice.dto.SystemMetrics;
import com.monitor.hostservice.service.SerialCommunicationService;
import com.monitor.hostservice.service.SystemInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsScheduler {

    private final SystemInfoService systemInfoService;
    private final SerialCommunicationService serialService;

    @Scheduled(fixedRateString = "${app.monitor.update-interval-ms:1000}")
    public void readAndLogMetrics() {
        try {
            SystemMetrics metrics = systemInfoService.getMetrics();

            String cpuStr = String.format(Locale.US, "%.1f", metrics.cpuLoadPercent());
            String ramStr = String.format(Locale.US, "%.1f", metrics.ramUsagePercent());
            String tempStr = String.format(Locale.US, "%.1f", metrics.cpuTemperature());
            String gpuLoadStr = String.format(Locale.US, "%.1f", metrics.gpuLoadPercent());

            log.info("Sys Status -> CPU: {}% ({} Cores, Temp: {}°C) | RAM: {}% (Used: {}GB / Total: {}GB) | GPU: {}% (Used: {}GB / Total: {}GB)",
                    cpuStr,
                    metrics.coreCount(),
                    tempStr,
                    ramStr,
                    metrics.usedRamGigabytes(),
                    metrics.totalRamGigabytes(),
                    gpuLoadStr,
                    metrics.usedGpuVramGigabytes(),
                    metrics.gpuVramGigabytes());

            String serialData = metrics.toSerialFormat();
            log.debug("Serial Data: {}", serialData.trim());

            serialService.sendData(serialData);

        } catch (Exception e) {
            log.error("Error occurred during metrics reading cycle: ", e);
        }
    }
}