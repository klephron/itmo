package ru.itmo.zavar;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.google.common.util.concurrent.AtomicDouble;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CustomMetricsCollector {
    private final DockerClient dockerClient;
    private final PrometheusMeterRegistry prometheusRegistry;

    public CustomMetricsCollector() {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://host.docker.internal:2375")
                .build();

        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        this.dockerClient = com.github.dockerjava.core.DockerClientImpl.getInstance(config, httpClient);
        this.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    public void startHttpServer(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/metrics", exchange -> {
            String response = prometheusRegistry.scrape();
            exchange.getResponseHeaders().set("Content-Type", "text/plain; version=0.0.4; charset=utf-8");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        server.start();
        System.out.println("Metrics server started on port " + port);
    }

    public void collectMetricsFromServices() {
        try {
            List<Container> containers = dockerClient.listContainersCmd().exec();

            Set<String> allActiveKeys = new HashSet<>();

            for (Container container : containers) {
                String containerId = container.getId();
                String containerName = container.getNames()[0];

                containerName = containerName.startsWith("/") ? containerName.substring(1) : containerName;

                if (!(containerName.contains("iot-controller") || containerName.contains("rule-engine"))) {
                    continue;
                }

                String metricsEndpoint = getMetricsEndpoint(container);
                if (metricsEndpoint != null) {
                    System.out.println("Fetching metrics from: " + containerName);
                    String metrics = fetchMetricsFromService(metricsEndpoint);

                    if (metrics != null) {
                        System.out.println("Metrics fetched from " + containerName + ":\n" + metrics);

                        Set<String> containerActiveKeys = parseAndRegisterMetrics(containerName, metrics);

                        allActiveKeys.addAll(containerActiveKeys);
                    } else {
                        System.err.println("Failed to fetch metrics from: " + containerName);
                    }
                }
            }

            removeStaleMetrics(allActiveKeys);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeStaleMetrics(Set<String> allActiveKeys) {
        Set<String> staleGaugeKeys = gaugeValues.keySet()
                .stream()
                .filter(key -> !allActiveKeys.contains(key))
                .collect(Collectors.toSet());

        Set<String> staleCounterKeys = counterValues.keySet().stream()
                .filter(key -> !allActiveKeys.contains(key))
                .collect(Collectors.toSet());

        for (String staleKey : staleGaugeKeys) {
            gaugeValues.remove(staleKey);
            removeMeterByKey(staleKey);
        }

        for (String staleKey : staleCounterKeys) {
            io.micrometer.core.instrument.Counter c = counterValues.remove(staleKey);
            if (c != null) {
                removeMeterByKey(staleKey);
            }
        }

    }

    private void removeMeterByKey(String staleKey) {
        int colonIndex = staleKey.lastIndexOf(':');
        if (colonIndex > 0) {
            String metricName = staleKey.substring(0, colonIndex);
            String containerName = staleKey.substring(colonIndex + 1);

            var meters = prometheusRegistry
                    .find(metricName)
                    .tag("container", containerName)
                    .meters();

            for (var meter : meters) {
                System.out.println("Removing meter: " + meter.getId());
                prometheusRegistry.remove(meter);
            }
        }
    }


    private final ConcurrentHashMap<String, AtomicDouble> gaugeValues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> counterValues = new ConcurrentHashMap<>();

    private Set<String> parseAndRegisterMetrics(String containerName, String metrics) {
        String currentMetricType = null;
        String currentMetricName = null;

        Set<String> activeKeys = new HashSet<>();

        for (String line : metrics.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("# TYPE")) {
                String[] typeParts = line.split(" ");
                if (typeParts.length == 4) {
                    currentMetricName = typeParts[2];
                    currentMetricType = typeParts[3];
                }
                continue;
            }

            if (line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split(" ");
            if (parts.length == 2) {
                String metricName = parts[0];
                double value;
                try {
                    value = Double.parseDouble(parts[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid metric value for " + metricName + ": " + parts[1]);
                    continue;
                }

                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    System.err.println("Metric value is NaN/Infinite for " + metricName);
                    value = 0.0;
                }

                String key = metricName + ":" + containerName;
                activeKeys.add(key);

                if ("gauge".equals(currentMetricType) || "counter".equals(currentMetricType)) {
                    double finalValue1 = value;
                    gaugeValues.computeIfAbsent(key, k -> {
                        AtomicDouble atomicValue = new AtomicDouble(finalValue1);
                        prometheusRegistry.gauge(metricName, Tags.of("container", containerName), atomicValue, AtomicDouble::get);
                        return atomicValue;
                    }).set(value);
                } else if ("summary".equals(currentMetricType)) {
                    if (metricName.endsWith("_sum") || metricName.endsWith("_max")) {
                        double finalValue = value;
                        gaugeValues.computeIfAbsent(key, k -> {
                            AtomicDouble atomicValue = new AtomicDouble(finalValue);
                            prometheusRegistry.gauge(metricName, Tags.of("container", containerName), atomicValue, AtomicDouble::get);
                            return atomicValue;
                        }).set(value);
                    } else if (metricName.endsWith("_count")) {
                        Counter counter = counterValues.computeIfAbsent(key, k ->
                                prometheusRegistry.counter(metricName, Tags.of("container", containerName))
                        );
                        counter.increment(value);
                    }
                } else {
                    System.err.println("Unknown metric type for " + metricName + ": " + currentMetricType);
                }
            }
        }
        return activeKeys;
    }





    private String getMetricsEndpoint(Container container) {
        return "http://" + container.getNetworkSettings().getNetworks().values().iterator().next().getIpAddress() + ":8087/metrics";
    }

    private String fetchMetricsFromService(String url) {
        try {
            if (!isEndpointReachable(url)) {
                System.err.println("Endpoint is not reachable: " + url);
                return null;
            }

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    return reader.lines().collect(Collectors.joining("\n"));
                }
            } else {
                System.err.println("Failed to fetch metrics from " + url + ". Response code: " + responseCode);
            }
        } catch (Exception e) {
            System.err.println("Exception while fetching metrics from: " + url + ". " + e.getMessage());
        }
        return null;
    }


    private boolean isEndpointReachable(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.connect();
            return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            System.err.println("Ping failed for endpoint: " + url);
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        CustomMetricsCollector collector = new CustomMetricsCollector();
        collector.startHttpServer(8080);
        new Thread(() -> {
            while (true) {
                collector.collectMetricsFromServices();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}