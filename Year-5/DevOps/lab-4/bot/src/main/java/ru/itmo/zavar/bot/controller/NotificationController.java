package ru.itmo.zavar.bot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.bot.dto.NotificationRequest;
import ru.itmo.zavar.bot.service.TelegramService;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotificationController {

    private final TelegramService telegramService;

    @PostMapping
    public ResponseEntity<String> sendNotification(@RequestBody NotificationRequest request) {
        telegramService.sendMessageToAll(request.getMessage());
        return ResponseEntity.ok("Notification sent to all subscribers");
    }
}
