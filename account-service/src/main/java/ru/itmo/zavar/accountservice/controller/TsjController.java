package ru.itmo.zavar.accountservice.controller;

import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.accountservice.service.MqttService;

@AllArgsConstructor
@RestController("/tsj")
public class TsjController {
    private final MqttService mqttService;

    @GetMapping("/test")
    public void me() {
        mqttService.publish("reset");
    }
}
