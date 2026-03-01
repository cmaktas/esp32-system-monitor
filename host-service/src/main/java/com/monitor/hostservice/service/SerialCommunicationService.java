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
        if (availablePorts.length == 0) {
            log.warn("No serial ports found attached to the system.");
            return;
        }

        log.info("Starting handshake protocol...");

        for (SerialPort port : availablePorts) {
            // Only test potential USB devices (skip Bluetooth, etc.)
            String name = port.getSystemPortName().toLowerCase();
            if (!name.contains("usb") && !name.startsWith("com") && !name.contains("ch340") && !name.contains("cp210")) {
                continue;
            }

            log.info("Testing port: {}", port.getSystemPortName());
            port.setBaudRate(appProperties.getSerial().getBaudRate());

            // Set 1-second timeout for read/write operations during handshake
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 1000, 1000);

            if (port.openPort()) {
                try {
                    // Send handshake ping
                    String pingMsg = "PING_MONITOR\n";
                    port.getOutputStream().write(pingMsg.getBytes(StandardCharsets.UTF_8));
                    port.getOutputStream().flush();

                    // Wait half a second for ESP32 to respond
                    Thread.sleep(500);

                    if (port.bytesAvailable() > 0) {
                        byte[] readBuffer = new byte[port.bytesAvailable()];
                        port.getInputStream().read(readBuffer);
                        String response = new String(readBuffer, StandardCharsets.UTF_8).trim();

                        // Did we get the expected response?
                        if (response.contains("ACK_MONITOR")) {
                            log.info(">>> SUCCESS! Device found. Connected to port: {}", port.getSystemPortName());
                            activePort = port;
                            outputStream = port.getOutputStream();

                            // Revert timeout for normal asynchronous communication
                            activePort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 100, 0);
                            return;
                        }
                    }
                } catch (Exception e) {
                    log.error("Error occurred while testing port: {}", port.getSystemPortName(), e);
                }

                // Close port and move to the next if no response
                port.closePort();
            }
        }

        log.error("No device responded to PING_MONITOR. Is the device plugged in?");
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