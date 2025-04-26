package ru.itmo.zavar.devopslabs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.itmo.zavar.devopslabs.dto.NotificationRequest;

@Service
@Slf4j
public class NotifyService {
    @Value("${notify-service.endpoint}")
    private String endpoint;

    private final RestTemplate restTemplate;

    public NotifyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Async
    public void sendMessage(String message) {
        try {
            NotificationRequest request = new NotificationRequest();
            request.setMessage(message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<NotificationRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(endpoint, entity, Void.class);
        } catch (Exception e) {
            log.error("Failed to send message to notification service: {}", e.getMessage());
        }
    }
}
