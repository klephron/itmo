package ru.itmo.zavar.accountservice.controller;

import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.accountservice.dto.BillDto;
import ru.itmo.zavar.accountservice.entity.BillEntity;
import ru.itmo.zavar.accountservice.service.BillService;
import ru.itmo.zavar.accountservice.service.MqttService;

import java.util.List;

@AllArgsConstructor
@RestController("/tsj")
public class TsjController {
    private final MqttService mqttService;
    private final BillService billService;

    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @GetMapping("/reset")
    public void me() {
        this.mqttService.publish("reset");
    }

    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @PostMapping("/generateBills")
    public List<BillDto> generateBills(@RequestParam long fromTimestamp, @RequestParam long toTimestamp, @RequestParam(defaultValue = "1800") long tariff) {
        return this.billService.generateBills(fromTimestamp, toTimestamp, tariff);
    }
}
