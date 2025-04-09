package ru.itmo.zavar.accountservice.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.accountservice.dto.PayHistoryDto;
import ru.itmo.zavar.accountservice.mapper.PayHistoryMapper;
import ru.itmo.zavar.accountservice.entity.BillEntity;
import ru.itmo.zavar.accountservice.entity.PayHistoryEntity;
import ru.itmo.zavar.accountservice.exception.ObjectNotFoundException;
import ru.itmo.zavar.accountservice.repository.BillRepository;
import ru.itmo.zavar.accountservice.repository.PayHistoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class PayService {
    private final PayHistoryRepository payHistoryRepository;
    private final PayHistoryMapper payHistoryMapper;
    private final BillRepository billRepository;

    @Transactional
    public PayHistoryDto pay(String billId) throws ObjectNotFoundException {
        BillEntity bill = this.billRepository.findById(billId).orElseThrow(() -> new ObjectNotFoundException("Bill not found"));
        bill.setPayed(true);
        this.billRepository.save(bill);
        PayHistoryEntity payHistoryEntity = new PayHistoryEntity();
        payHistoryEntity.setBillId(billId);
        payHistoryEntity.setUserId(bill.getUserId());
        payHistoryEntity.setTimestamp(System.currentTimeMillis());
        PayHistoryEntity saved = this.payHistoryRepository.save(payHistoryEntity);
        return this.payHistoryMapper.toDto(saved);
    }

    public List<PayHistoryDto> getPayHistoryByUserId(String userId) throws ObjectNotFoundException {
        List<PayHistoryEntity> allByUserId = this.payHistoryRepository.findAllByUserId(userId);
        return allByUserId.stream().map(payHistoryMapper::toDto).toList();
    }
}
