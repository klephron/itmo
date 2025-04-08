package ru.itmo.zavar.accountservice.service;

import org.springframework.stereotype.Service;
import ru.itmo.zavar.accountservice.dto.BillDto;
import ru.itmo.zavar.accountservice.entity.BillEntity;
import ru.itmo.zavar.accountservice.exception.ObjectNotFoundException;
import ru.itmo.zavar.accountservice.mapper.BillMapper;
import ru.itmo.zavar.accountservice.repository.BillRepository;

import java.util.List;

@Service
public class BillService {
    private final BillRepository billRepository;
    private final BillMapper billMapper;

    public BillService(BillRepository billRepository, BillMapper billMapper) {
        this.billRepository = billRepository;
        this.billMapper = billMapper;
    }

    public BillDto getBillById(String id) throws ObjectNotFoundException {
        BillEntity bill = this.billRepository.findById(id).orElseThrow(() -> new ObjectNotFoundException("Bill not found"));
        return this.billMapper.toDto(bill);
    }

    public List<BillDto> getAllBills() {
        List<BillEntity> bills = this.billRepository.findAll();
        return bills.stream().map(this.billMapper::toDto).toList();
    }

    public List<BillDto> getAllBillsByUserId(String userId) {
        List<BillEntity> bills = this.billRepository.findByUserId(userId);
        return bills.stream().map(this.billMapper::toDto).toList();
    }

    public BillDto createBill(String userId, Long heat, Long cost) {
        BillEntity bill = new BillEntity();
        bill.setUserId(userId);
        bill.setHeat(heat);
        bill.setCost(cost);
        bill.setPayed(false);
        bill.setTimestamp(System.currentTimeMillis());
        BillEntity save = this.billRepository.save(bill);
        return this.billMapper.toDto(save);
    }

}
