package ru.itmo.zavar.bot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.bot.service.ChatIdService;

import java.util.Map;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final ChatIdService chatIdService;

    @PostMapping
    public void receiveUpdate(@RequestBody Map<String, Object> update) {
        try {
            Map<String, Object> message = (Map<String, Object>) update.get("message");
            if (message != null) {
                Map<String, Object> chat = (Map<String, Object>) message.get("chat");
                if (chat != null) {
                    Long chatId = ((Number) chat.get("id")).longValue();
                    chatIdService.saveChatId(chatId);
                    System.out.println("Received new chatId: " + chatId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing update: " + e.getMessage());
        }
    }
}