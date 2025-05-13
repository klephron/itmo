package ru.itmo.zavar.accountservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.accountservice.dto.BillDto;
import ru.itmo.zavar.accountservice.dto.HeatCostDto;
import ru.itmo.zavar.accountservice.dto.PayHistoryDto;
import ru.itmo.zavar.accountservice.security.CustomUserDetails;
import ru.itmo.zavar.accountservice.service.BillService;
import ru.itmo.zavar.accountservice.service.PayService;

import java.util.List;

@RestController
@RequestMapping("/user")
@Tag(name = "User API", description = "Методы для пользователей и ТСЖ")
public class UserController {

    private final BillService billService;
    private final PayService payService;

    public UserController(BillService billService, PayService payService) {
        this.billService = billService;
        this.payService = payService;
    }

    @Operation(summary = "Получить счёт по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Счёт найден", content = @Content(schema = @Schema(implementation = BillDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка при получении")
    })
    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @GetMapping("/bills/{id}")
    public ResponseEntity<?> getBillById(
            @Parameter(description = "ID пользователя") @PathVariable String id) {
        try {
            BillDto bill = this.billService.getBillById(id);
            return ResponseEntity.ok(bill);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Получить все счета")
    @ApiResponse(responseCode = "200", description = "Список счетов", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BillDto.class))))
    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @GetMapping("/bills")
    public ResponseEntity<List<BillDto>> getAllBills() {
        List<BillDto> bills = this.billService.getAllBills();
        return ResponseEntity.ok(bills);
    }

    @Operation(summary = "Получить счета пользователя по ID")
    @ApiResponse(responseCode = "200", description = "Список счетов", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BillDto.class))))
    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @GetMapping("/bills/by-user/{userId}")
    public ResponseEntity<List<BillDto>> getAllBillsByUserId(
            @Parameter(description = "ID пользователя") @PathVariable String userId) {
        List<BillDto> bills = this.billService.getAllBillsByUserId(userId);
        return ResponseEntity.ok(bills);
    }

    @Operation(summary = "Получить счета текущего пользователя")
    @ApiResponse(responseCode = "200", description = "Список счетов", content = @Content(array = @ArraySchema(schema = @Schema(implementation = BillDto.class))))
    @GetMapping("/bills/my")
    public ResponseEntity<List<BillDto>> getAllBillsByUserId(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user) {
        List<BillDto> bills = this.billService.getAllBillsByUserId(user.getId());
        return ResponseEntity.ok(bills);
    }

    @Operation(summary = "Получить историю затрат на отопление для текущего пользователя")
    @ApiResponse(responseCode = "200", description = "Список затрат", content = @Content(array = @ArraySchema(schema = @Schema(implementation = HeatCostDto.class))))
    @GetMapping("/bills/heat-cost")
    public ResponseEntity<List<HeatCostDto>> getHeatCost(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user) {
        List<BillDto> bills = this.billService.getAllBillsByUserId(user.getId());
        List<HeatCostDto> list = bills.stream().map(billDto -> new HeatCostDto(billDto.getTimestamp(), billDto.getHeat(), billDto.getCost())).toList();
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Оплатить счёт")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Счёт успешно оплачен", content = @Content(schema = @Schema(implementation = PayHistoryDto.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка оплаты")
    })
    @PostMapping("/pay/by-bill/{billId}")
    public ResponseEntity<?> payBill(
            @Parameter(description = "ID пользователя") @PathVariable String billId,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user) {
        try {
            PayHistoryDto result = this.payService.pay(billId, user.getId());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Operation(summary = "Получить историю оплат пользователя по ID")
    @ApiResponse(responseCode = "200", description = "История оплат", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PayHistoryDto.class))))
    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @GetMapping("/pay/history-by-user/{userId}")
    public ResponseEntity<List<PayHistoryDto>> getUserPayHistory(
            @Parameter(description = "ID пользователя") @PathVariable String userId) {
        List<PayHistoryDto> history = this.payService.getPayHistoryByUserId(userId);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Получить историю оплат текущего пользователя")
    @ApiResponse(responseCode = "200", description = "История оплат", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PayHistoryDto.class))))
    @GetMapping("/pay/history")
    public ResponseEntity<List<PayHistoryDto>> geHistory(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user) {
        List<PayHistoryDto> history = this.payService.getPayHistoryByUserId(user.getId());
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "Получить имя текущего пользователя")
    @ApiResponse(responseCode = "200", description = "Имя пользователя", content = @Content(schema = @Schema(implementation = String.class)))
    @GetMapping("/me")
    public String me(@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user) {
        return user.getUsername();
    }
}