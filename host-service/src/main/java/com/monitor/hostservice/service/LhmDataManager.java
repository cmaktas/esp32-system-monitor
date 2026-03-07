package com.monitor.hostservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monitor.hostservice.config.AppProperties;
import com.monitor.hostservice.dto.LhmNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LhmDataManager {

    private final HttpClient httpClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private LhmNode rootNode;

    public LhmDataManager(AppProperties appProperties) {
        this.appProperties = appProperties;

        this.objectMapper = new ObjectMapper();

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(200))
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(
                                appProperties.getLhm().getUsername(),
                                appProperties.getLhm().getPassword().toCharArray());
                    }
                }).build();
    }

    public void refresh() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(appProperties.getLhm().getUrl()))
                    .timeout(Duration.ofMillis(200)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            this.rootNode = objectMapper.readValue(response.body(), LhmNode.class);
        } catch (Exception e) {
            log.trace("LHM link unavailable");
            this.rootNode = null;
        }
    }

    // --- CPU
    public double getCpuTemp() { return extract(findNode(rootNode, "/amdcpu/0/temperature/2")); }
    public double getCpuPower() { return extract(findNode(rootNode, "/amdcpu/0/power/0")); }

    // --- GPU
    public double getGpuLoad() { return extract(findNode(rootNode, "/gpu-nvidia/0/load/0")); }
    public double getGpuTemp() { return extract(findNode(rootNode, "/gpu-nvidia/0/temperature/0")); }
    public double getGpuPower() { return extract(findNode(rootNode, "/gpu-nvidia/0/power/0")); }
    public int getGpuClock() { return (int) extract(findNode(rootNode, "/gpu-nvidia/0/clock/0")); }

    // VRAM
    public long getGpuUsedVram() { return (long) (extract(findNode(rootNode, "/gpu-nvidia/0/smalldata/1")) / 1024.0); }
    public long getGpuTotalVram() { return (long) (extract(findNode(rootNode, "/gpu-nvidia/0/smalldata/2")) / 1024.0); }

    // --- RAM
    public double getRamTemp() {
        List<LhmNode> temps = new ArrayList<>();
        collectNodesByText(rootNode, "DIMM #", temps);
        return temps.stream().mapToDouble(this::extract).average().orElse(0.0);
    }

    public double getRamPower() {
        return extract(findNode(rootNode, "/ram/power/0"));
    }

    private LhmNode findNode(LhmNode current, String sensorId) {
        if (current == null || sensorId == null) return null;
        if (sensorId.equals(current.getSensorId())) return current;
        if (current.getChildren() != null) {
            for (LhmNode child : current.getChildren()) {
                LhmNode found = findNode(child, sensorId);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void collectNodesByText(LhmNode current, String prefix, List<LhmNode> results) {
        if (current == null) return;
        if (current.getText() != null && current.getText().startsWith(prefix) && current.getSensorId() != null) {
            results.add(current);
        }
        if (current.getChildren() != null) {
            for (LhmNode child : current.getChildren()) {
                collectNodesByText(child, prefix, results);
            }
        }
    }

    private double extract(LhmNode node) {
        if (node == null || node.getValue() == null) return 0.0;
        try {
            String val = node.getValue().replaceAll("[^\\d.,]", "").replace(",", ".");
            return val.isEmpty() ? 0.0 : Double.parseDouble(val);
        } catch (Exception e) { return 0.0; }
    }
}