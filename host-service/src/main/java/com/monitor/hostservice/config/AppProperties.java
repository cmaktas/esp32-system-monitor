package com.monitor.hostservice.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Slf4j
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Monitor monitor = new Monitor();
    private final Serial serial = new Serial();

    @Data
    public static class Monitor {
        private long updateIntervalMs;
    }

    @Data
    public static class Serial {
        private String portName;
        private int baudRate;
    }

    @PostConstruct
    public void init() {
        log.info("Hardware Monitor initialized with settings: Update Interval={}ms, Port={}, BaudRate={}",
                monitor.getUpdateIntervalMs(),
                serial.getPortName(),
                serial.getBaudRate());
    }
}