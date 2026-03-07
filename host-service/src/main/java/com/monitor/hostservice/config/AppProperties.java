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
    private final Lhm lhm = new Lhm();

    @Data
    public static class Monitor {
        private long updateIntervalMs;
    }

    @Data
    public static class Serial {
        private String portName;
        private int baudRate;
    }

    @Data
    public static class Lhm {
        private String url;
        private String username;
        private String password;
    }

    @PostConstruct
    public void init() {
        log.info("Hardware Monitor initialized with settings: Update Interval={}ms, Port={}, BaudRate={}, LHM URL={}",
                monitor.getUpdateIntervalMs(),
                serial.getPortName(),
                serial.getBaudRate(),
                lhm.getUrl());

        if (lhm.getUsername() == null || lhm.getPassword() == null) {
            log.warn("LHM Basic Auth credentials are not set! Ensure environment variables are configured.");
        }
    }
}