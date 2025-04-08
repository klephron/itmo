package ru.itmo.zavar.accountservice.controller;

import org.springframework.http.ResponseEntity;
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

    @GetMapping("/bills/{id}")
    public ResponseEntity<BillDto> getBillById(@PathVariable String id) throws ObjectNotFoundException {
        BillDto bill = this.billService.getBillById(id);
        return ResponseEntity.ok(bill);
    }

    @GetMapping("/bills")
    public ResponseEntity<List<BillDto>> getAllBills() {
        List<BillDto> bills = this.billService.getAllBills();
        return ResponseEntity.ok(bills);
    }

    @GetMapping("/bills/by-user/{userId}")
    public ResponseEntity<List<BillDto>> getAllBillsByUserId(@PathVariable String userId) {
        List<BillDto> bills = this.billService.getAllBillsByUserId(userId);
        return ResponseEntity.ok(bills);
    }

    @PostMapping("/pay/by-bill/{billId}")
    public ResponseEntity<PayHistoryDto> payBill(@PathVariable String billId) throws ObjectNotFoundException {
        PayHistoryDto result = this.payService.pay(billId);
        return ResponseEntity.ok(result);
    }

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