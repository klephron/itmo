package ru.itmo.zavar.accountservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.accountservice.dto.PayHistoryDto;
import ru.itmo.zavar.accountservice.entity.BillEntity;
import ru.itmo.zavar.accountservice.entity.PayHistoryEntity;
import ru.itmo.zavar.accountservice.entity.UserEntity;
import ru.itmo.zavar.accountservice.exception.ObjectNotFoundException;
import ru.itmo.zavar.accountservice.mapper.PayHistoryMapper;
import ru.itmo.zavar.accountservice.repository.BillRepository;
import ru.itmo.zavar.accountservice.repository.PayHistoryRepository;
import ru.itmo.zavar.accountservice.repository.UserRepository;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayServiceTest {

    private PayHistoryRepository payHistoryRepository;
    private PayHistoryMapper payHistoryMapper;
    private BillRepository billRepository;
    private UserRepository userRepository;
    private PayService payService;

    @BeforeEach
    void setUp() {
        payHistoryRepository = mock(PayHistoryRepository.class);
        payHistoryMapper = mock(PayHistoryMapper.class);
        billRepository = mock(BillRepository.class);
        userRepository = mock(UserRepository.class);

        payService = new PayService(
                payHistoryRepository,
                payHistoryMapper,
                billRepository,
                userRepository
        );
    }

    @Test
    void testPaySuccess() throws Exception {
        UserEntity user = new UserEntity();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        BillEntity bill = new BillEntity();
        bill.setId("bill-1");
        bill.setUserId("user-1");
        when(billRepository.findById("bill-1")).thenReturn(Optional.of(bill));

        when(payHistoryRepository.save(any())).thenAnswer(inv -> {
            PayHistoryEntity phe = inv.getArgument(0);
            phe.setId("pay-1");
            return phe;
        });

        PayHistoryDto dto = new PayHistoryDto("pay-1", "bill-1", System.currentTimeMillis());
        when(payHistoryMapper.toDto(any())).thenReturn(dto);

        PayHistoryDto result = payService.pay("bill-1", "user-1");
        assertNotNull(result);
        assertEquals("pay-1", result.getId());
        verify(billRepository).save(bill);
        assertTrue(bill.getPayed());
    }

    @Test
    void testPayUserNotFound() {
        when(userRepository.findById("user-1")).thenReturn(Optional.empty());
        assertThrows(ObjectNotFoundException.class, () ->
                payService.pay("bill-1", "user-1"));
    }

    @Test
    void testPayBillNotFound() {
        UserEntity user = new UserEntity();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        when(billRepository.findById("bill-1")).thenReturn(Optional.empty());
        assertThrows(ObjectNotFoundException.class, () ->
                payService.pay("bill-1", "user-1"));
    }

    @Test
    void testPayWrongUser() {
        UserEntity user = new UserEntity();
        user.setId("user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        BillEntity bill = new BillEntity();
        bill.setId("bill-1");
        bill.setUserId("user-999");
        when(billRepository.findById("bill-1")).thenReturn(Optional.of(bill));

        assertThrows(IllegalAccessException.class, () ->
                payService.pay("bill-1", "user-1"));
    }

    @Test
    void testGetPayHistoryByUserId() throws ObjectNotFoundException {
        PayHistoryEntity phe1 = new PayHistoryEntity();
        phe1.setId("pay-1");
        phe1.setUserId("user-1");
        phe1.setBillId("bill-1");

        when(payHistoryRepository.findAllByUserId("user-1"))
                .thenReturn(List.of(phe1));

        PayHistoryDto dto1 = new PayHistoryDto("pay-1", "bill-1", System.currentTimeMillis());
        when(payHistoryMapper.toDto(phe1)).thenReturn(dto1);

        List<PayHistoryDto> result = payService.getPayHistoryByUserId("user-1");
        assertEquals(1, result.size());
        assertEquals("pay-1", result.get(0).getId());
    }
}
