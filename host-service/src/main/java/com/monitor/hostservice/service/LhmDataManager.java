package com.monitor.hostservice.service;

import com.monitor.hostservice.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LhmDataManager {

    private final HttpClient httpClient;
    private final AppProperties appProperties;
    private String currentJson = "";

    // CPU Patterns
    private static final Pattern CPU_TEMP = Pattern.compile("\"Text\"\\s*:\\s*\"(?:Core \\(Tctl/Tdie\\)|CPU Package)\"[^}]*\"Value\"\\s*:\\s*\"([\\d,.]+)\\s*°C\"");
    private static final Pattern CPU_POWER = Pattern.compile("\"Text\"\\s*:\\s*\"Package\"[^}]*\"Value\"\\s*:\\s*\"([\\d,.]+)\\s*W\"");

    // GPU Patterns
    private static final Pattern GPU_LOAD = Pattern.compile("\"Text\"\\s*:\\s*\"GPU Core\"[^}]*\"SensorType\"\\s*:\\s*\"Load\"[^}]*\"Value\"\\s*:\\s*\"([\\d,.]+)\\s*%\"");
    private static final Pattern GPU_TEMP = Pattern.compile("\"Text\"\\s*:\\s*\"GPU Core\"[^}]*\"SensorType\"\\s*:\\s*\"Temperature\"[^}]*\"Value\"\\s*:\\s*\"([\\d,.]+)\\s*°C\"");
    private static final Pattern GPU_POWER = Pattern.compile("\"Text\"\\s*:\\s*\"GPU Package\"[^}]*\"Value\"\\s*:\\s*\"([\\d,.]+)\\s*W\"");
    private static final Pattern GPU_CLOCK = Pattern.compile("\"Text\"\\s*:\\s*\"GPU Core\"[^}]*\"SensorType\"\\s*:\\s*\"Clock\"[^}]*\"Value\"\\s*:\\s*\"([\\d,.]+)\\s*MHz\"");
    private static final Pattern GPU_VRAM_USED = Pattern.compile("\"Text\"\\s*:\\s*\"GPU Memory Used\"[^}]*\"Value\"\\s*:\\s*\"([\\d,.]+)\\s*MB\"");
    private static final Pattern GPU_VRAM_TOTAL = Pattern.compile("\"Text\"\\s*:\\s*\"GPU Memory Total\"[^}]*\"Value\"\\s*:\\s*\"([\\d,.]+)\\s*MB\"");

    // RAM Patterns
    private static final Pattern RAM_TEMP = Pattern.compile("\"Text\"\\s*:\\s*\"Memory [A-Z\\d]+\"[^}]*\"Value\"\\s*:\\s*\"([\\d,.]+)\\s*°C\"");
    private static final Pattern RAM_POWER = Pattern.compile("\"SensorType\"\\s*:\\s*\"Power\"[^}]*\"Identifier\"\\s*:\\s*\"/ram/[^}]*\"Value\"\\s*:\\s*\"([\\d,.]+)\\s*W\"");

    public LhmDataManager(AppProperties appProperties) {
        this.appProperties = appProperties;
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
            this.currentJson = httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
        } catch (Exception e) {
            this.currentJson = "";
        }
    }

    // Public Getters
    public double getCpuTemp() { return extract(CPU_TEMP); }
    public double getCpuPower() { return extract(CPU_POWER); }
    public double getGpuLoad() { return extract(GPU_LOAD); }
    public double getGpuTemp() { return extract(GPU_TEMP); }
    public double getGpuPower() { return extract(GPU_POWER); }
    public int getGpuClock() { return (int) extract(GPU_CLOCK); }
    public long getGpuUsedVram() { return (long) (extract(GPU_VRAM_USED) / 1024); } // MB to GB
    public long getGpuTotalVram() { return (long) (extract(GPU_VRAM_TOTAL) / 1024); }
    public double getRamTemp() { return extract(RAM_TEMP); }
    public double getRamPower() { return extract(RAM_POWER); }

    private double extract(Pattern pattern) {
        if (currentJson.isEmpty()) return 0.0;
        Matcher m = pattern.matcher(currentJson);
        return m.find() ? Double.parseDouble(m.group(1).replace(',', '.')) : 0.0;
    }
}