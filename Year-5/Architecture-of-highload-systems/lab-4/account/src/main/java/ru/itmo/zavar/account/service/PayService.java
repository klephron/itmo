package ru.itmo.zavar.account.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.account.dto.PayHistoryDto;
import ru.itmo.zavar.account.entity.UserEntity;
import ru.itmo.zavar.account.mapper.PayHistoryMapper;
import ru.itmo.zavar.account.entity.BillEntity;
import ru.itmo.zavar.account.entity.PayHistoryEntity;
import ru.itmo.zavar.account.exception.ObjectNotFoundException;
import ru.itmo.zavar.account.repository.BillRepository;
import ru.itmo.zavar.account.repository.PayHistoryRepository;
import ru.itmo.zavar.account.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j(topic = "AS-PayService")
@AllArgsConstructor
@Service
public class PayService {
    private final PayHistoryRepository payHistoryRepository;
    private final PayHistoryMapper payHistoryMapper;
    private final BillRepository billRepository;
    private final UserRepository userRepository;

    @Transactional
    public PayHistoryDto pay(String billId, String userId) throws ObjectNotFoundException, IllegalAccessException {
        this.userRepository.findById(userId).orElseThrow(() -> new ObjectNotFoundException("User not found"));
        BillEntity bill = this.billRepository.findById(billId).orElseThrow(() -> new ObjectNotFoundException("Bill not found"));
        if (!bill.getUserId().equals(userId)) {
            throw new IllegalAccessException("You don't own this bill");
        }
        bill.setPayed(true);
        this.billRepository.save(bill);
        PayHistoryEntity payHistoryEntity = new PayHistoryEntity();
        payHistoryEntity.setBillId(billId);
        payHistoryEntity.setUserId(bill.getUserId());
        payHistoryEntity.setTimestamp(System.currentTimeMillis());
        PayHistoryEntity saved = this.payHistoryRepository.save(payHistoryEntity);
        log.info("Payed {} bill", saved.getBillId());
        return this.payHistoryMapper.toDto(saved);
    }

    public List<PayHistoryDto> getPayHistoryByUserId(String userId) throws ObjectNotFoundException {
        List<PayHistoryEntity> allByUserId = this.payHistoryRepository.findAllByUserId(userId);
        return allByUserId.stream().map(payHistoryMapper::toDto).toList();
    }
}
