package ru.itmo.zavar.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.account.dto.BillDto;
import ru.itmo.zavar.account.dto.HeatCostDto;
import ru.itmo.zavar.account.dto.HeatCostUserDto;
import ru.itmo.zavar.account.security.CustomUserDetails;
import ru.itmo.zavar.account.service.BillService;
import ru.itmo.zavar.account.service.MqttService;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/tsj")
@Tag(name = "ТСЖ", description = "Методы, доступные только пользователям с ролью ТСЖ")
public class TsjController {
    private final MqttService mqttService;
    private final BillService billService;

    @Operation(summary = "Сбросить данные (через MQTT)", description = "Отправляет сигнал на сброс всех датчиков")
    @ApiResponse(responseCode = "200", description = "Сброс выполнен")
    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.account.security.Roles).TSJ.toString())")
    @GetMapping("/reset")
    public ResponseEntity<?> me() {
        this.mqttService.publish("reset");
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Сгенерировать счета", description = "Генерирует счета за отопление за указанный период и уведомляет пользователей")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Счета успешно сгенерированы",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = BillDto.class))))
    })
    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.account.security.Roles).TSJ.toString())")
    @PostMapping("/generateBills")
    public ResponseEntity<List<BillDto>> generateBills(@Parameter(description = "Начальное время (timestamp)") @RequestParam long fromTimestamp,
                                                       @Parameter(description = "Конечное время (timestamp)") @RequestParam long toTimestamp,
                                                       @Parameter(description = "Тариф (по умолчанию 1800)") @RequestParam(defaultValue = "1800") long tariff) {
        List<BillDto> billDtos = this.billService.generateBills(fromTimestamp, toTimestamp, tariff);
        this.billService.notifyUsers(billDtos);
        this.mqttService.publish("reset");
        return ResponseEntity.ok(billDtos);
    }

    @Operation(summary = "Анализ потребления по пользователям", description = "Группирует счета по пользователям и возвращает график потребления")
    @ApiResponse(responseCode = "200", description = "Анализ успешно выполнен",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = HeatCostUserDto.class))))
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
