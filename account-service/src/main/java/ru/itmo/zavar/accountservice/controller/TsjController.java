package ru.itmo.zavar.accountservice.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.accountservice.dto.BillDto;
import ru.itmo.zavar.accountservice.dto.HeatCostDto;
import ru.itmo.zavar.accountservice.dto.HeatCostUserDto;
import ru.itmo.zavar.accountservice.security.CustomUserDetails;
import ru.itmo.zavar.accountservice.service.BillService;
import ru.itmo.zavar.accountservice.service.MqttService;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController("/tsj")
public class TsjController {
    private final MqttService mqttService;
    private final BillService billService;

    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @GetMapping("/reset")
    public ResponseEntity<?> me() {
        this.mqttService.publish("reset");
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @PostMapping("/generateBills")
    public ResponseEntity<List<BillDto>> generateBills(@RequestParam long fromTimestamp, @RequestParam long toTimestamp, @RequestParam(defaultValue = "1800") long tariff) {
        List<BillDto> billDtos = this.billService.generateBills(fromTimestamp, toTimestamp, tariff);
        this.billService.notifyUsers(billDtos);
        this.mqttService.publish("reset");
        return ResponseEntity.ok(billDtos);
    }

    @GetMapping("/analyze")
    public ResponseEntity<List<HeatCostUserDto>> analyze() {
        List<BillDto> bills = this.billService.getAllBills();
        return ResponseEntity.ok(groupBillsByUser(bills));
    }

    public List<HeatCostUserDto> groupBillsByUser(List<BillDto> bills) {
        return bills.stream()
                .collect(Collectors.groupingBy(BillDto::getUserId))
                .entrySet().stream()
                .map(entry -> new HeatCostUserDto(
                        entry.getKey(),
                        entry.getValue().stream().map(BillDto::getTimestamp).toList(),
                        entry.getValue().stream().map(BillDto::getHeat).toList(),
                        entry.getValue().stream().map(BillDto::getCost).toList()
                ))
                .toList();
    }


}
