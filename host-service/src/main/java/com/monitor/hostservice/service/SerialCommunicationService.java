package com.monitor.hostservice.service;

import com.fazecast.jSerialComm.SerialPort;
import com.monitor.hostservice.config.AppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

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
        SerialPort[] availablePorts = SerialPort.getCommPorts();
        log.info("Starting CONCURRENT handshake protocol on {} available ports...", availablePorts.length);

        Optional<SerialPort> connectedPort = Arrays.stream(availablePorts)
                .parallel()
                .map(this::attemptHandshake)
                .filter(java.util.Objects::nonNull)
                .findFirst();

        if (connectedPort.isPresent()) {
            activePort = connectedPort.get();
            outputStream = activePort.getOutputStream();
            activePort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 1000);
            log.info(">>> SUCCESS! Device found and locked on: {}", activePort.getSystemPortName());
        } else {
            log.error("No device responded to PING_MONITOR. Is the display plugged in?");
        }
    }

    private SerialPort attemptHandshake(SerialPort port) {
        String desc = port.getDescriptivePortName().toLowerCase();
        if (!desc.contains("usb") && !desc.contains("ch340") && !desc.contains("cp210") && !desc.contains("prolific")) {
            return null;
        }

        log.debug("Testing likely candidate: {}", port.getSystemPortName());
        port.setBaudRate(appProperties.getSerial().getBaudRate());

        port.clearDTR();
        port.clearRTS();

        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 300, 300);

        if (port.openPort(200)) {
            try {
                if (port.bytesAvailable() > 0) {
                    port.readBytes(new byte[port.bytesAvailable()], port.bytesAvailable());
                }

                String pingMsg = "REQ_ESP32_SYSMON_v1_0x7A9B\n";
                port.writeBytes(pingMsg.getBytes(StandardCharsets.UTF_8), pingMsg.length());

                Thread.sleep(200);

                if (port.bytesAvailable() > 0) {
                    byte[] readBuffer = new byte[port.bytesAvailable()];
                    port.readBytes(readBuffer, readBuffer.length);
                    String response = new String(readBuffer, StandardCharsets.UTF_8).trim();

                    if (response.contains("ACK_ESP32_SYSMON_v1_0x7A9B")) {
                        return port;
                    }
                }
            } catch (Exception e) {
                log.trace("Handshake failed on {}", port.getSystemPortName());
            }
            port.closePort();
        }
        return null;
    }

    public void sendData(String data) {
        if (activePort != null && activePort.isOpen() && outputStream != null) {
            try {
                outputStream.write(data.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (Exception e) {
                log.error("Failed to write data to serial port: ", e);
            }
        } else {
            log.trace("Serial port is closed, data transmission skipped.");
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