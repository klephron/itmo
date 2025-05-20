package ru.itmo.zavar.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class TelegramService {

    private final ChatIdService chatIdService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot-token}")
    private String botToken;

    public void sendMessageToAll(String message) {
        for (Long chatId : chatIdService.getAllChatIds()) {
            sendMessage(chatId, message);
        }
    }

    private void sendMessage(Long chatId, String message) {
        try {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
            String url = String.format(
                    "https://api.telegram.org/bot%s/sendMessage?chat_id=%d&text=%s",
                    botToken, chatId, encodedMessage
            );
            restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            System.err.println("Failed to send message to chatId " + chatId + ": " + e.getMessage());
        }
    }
}