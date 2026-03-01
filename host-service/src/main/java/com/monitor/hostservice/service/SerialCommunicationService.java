package com.monitor.hostservice.service;

import com.fazecast.jSerialComm.SerialPort;
import com.monitor.hostservice.config.AppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.OutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SerialCommunicationService {

    private final AppProperties appProperties;
    private SerialPort activePort;
    private OutputStream outputStream;

    @PostConstruct
    public void init() {
        connectToPort();
    }

    private void connectToPort() {
        String configuredPortName = appProperties.getSerial().getPortName();
        int baudRate = appProperties.getSerial().getBaudRate();

        SerialPort[] availablePorts = SerialPort.getCommPorts();
        if (availablePorts.length == 0) {
            log.warn("No serial ports found on the system. Is the device plugged in?");
            return;
        }

        if ("AUTO".equalsIgnoreCase(configuredPortName)) {
            activePort = availablePorts[0];
            for (SerialPort port : availablePorts) {
                String desc = port.getDescriptivePortName().toLowerCase();
                String sysName = port.getSystemPortName().toLowerCase();
                if (desc.contains("usb") || desc.contains("serial") || sysName.contains("usb") || sysName.contains("ch340")) {
                    activePort = port;
                    break;
                }
            }
            log.info("AUTO mode selected port: {} ({})", activePort.getSystemPortName(), activePort.getDescriptivePortName());
        } else {
            activePort = SerialPort.getCommPort(configuredPortName);
        }

        activePort.setBaudRate(baudRate);
        activePort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 100, 0);

        if (activePort.openPort()) {
            outputStream = activePort.getOutputStream();
            log.info("Successfully opened serial port: {}", activePort.getSystemPortName());
        } else {
            log.error("Failed to open serial port: {}", activePort.getSystemPortName());
        }
    }

    public void sendData(String data) {
        if (activePort != null && activePort.isOpen() && outputStream != null) {
            try {
                outputStream.write(data.getBytes());
                outputStream.flush();
            } catch (Exception e) {
                log.error("Error writing to serial port: ", e);
            }
        } else {
            log.trace("Serial port is not open. Skipping data transmission.");
        }
    }

    @PreDestroy
    public void cleanup() {
        if (activePort != null && activePort.isOpen()) {
            activePort.closePort();
            log.info("Serial port closed safely.");
        }
    }
}