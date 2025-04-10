package ru.itmo.zavar.accountservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.accountservice.dto.BillDto;
import ru.itmo.zavar.accountservice.dto.PayHistoryDto;
import ru.itmo.zavar.accountservice.exception.ObjectNotFoundException;
import ru.itmo.zavar.accountservice.service.BillService;
import ru.itmo.zavar.accountservice.service.PayService;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    private final BillService billService;
    private final PayService payService;

    public UserController(BillService billService, PayService payService) {
        this.billService = billService;
        this.payService = payService;
    }

    @PreAuthorize("hasAuthority('TSJ')")
    @GetMapping("/bills/{id}")
    public ResponseEntity<?> getBillById(@PathVariable String id) {
        try {
            BillDto bill = this.billService.getBillById(id);
            return ResponseEntity.ok(bill);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('TSJ')")
    @GetMapping("/bills")
    public ResponseEntity<List<BillDto>> getAllBills() {
        List<BillDto> bills = this.billService.getAllBills();
        return ResponseEntity.ok(bills);
    }

    @PreAuthorize("hasAuthority('TSJ')")
    @GetMapping("/bills/by-user/{userId}")
    public ResponseEntity<List<BillDto>> getAllBillsByUserId(@PathVariable String userId) {
        List<BillDto> bills = this.billService.getAllBillsByUserId(userId);
        return ResponseEntity.ok(bills);
    }

    @PostMapping("/pay/by-bill/{billId}")
    public ResponseEntity<?> payBill(@PathVariable String billId) {
        try {
            PayHistoryDto result = this.payService.pay(billId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority('TSJ')")
    @GetMapping("/pay/history-by-user/{userId}")
    public ResponseEntity<List<PayHistoryDto>> getUserPayHistory(@PathVariable String userId) {
        List<PayHistoryDto> history = this.payService.getPayHistoryByUserId(userId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/me")
    public String me(@AuthenticationPrincipal UserDetails user) {
        return user.getUsername();
    }
}