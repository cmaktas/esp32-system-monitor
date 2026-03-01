package com.monitor.hostservice.scheduler;

import com.monitor.hostservice.dto.SystemMetrics;
import com.monitor.hostservice.service.SerialCommunicationService;
import com.monitor.hostservice.service.SystemInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

            String cpuStr = String.format(java.util.Locale.US, "%.1f", metrics.cpuLoadPercent());
            String ramStr = String.format(java.util.Locale.US, "%.1f", metrics.ramUsagePercent());

            log.info("System Status -> CPU: {}% | RAM: {}% (Used: {}GB / Total: {}GB)",
                    cpuStr,
                    ramStr,
                    metrics.usedRamGigabytes(),
                    metrics.totalRamGigabytes());

            String serialData = metrics.toSerialFormat();
            log.debug("Data to be written to Serial Port: {}", serialData.trim());

            serialService.sendData(serialData);

        } catch (Exception e) {
            log.error("Error occurred during metrics reading cycle: ", e);
        }
    }
}