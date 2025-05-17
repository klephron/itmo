package ru.itmo.zavar.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import ru.itmo.zavar.account.dto.BillDto;
import ru.itmo.zavar.account.dto.UserDto;
import ru.itmo.zavar.account.entity.BillEntity;
import ru.itmo.zavar.account.exception.ObjectNotFoundException;
import ru.itmo.zavar.account.mapper.BillMapper;
import ru.itmo.zavar.account.repository.BillRepository;
import ru.itmo.zavar.account.service.BillService;
import ru.itmo.zavar.account.service.UserService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
public class BillServiceTest {

    private BillRepository billRepository;
    private BillMapper billMapper;
    private MongoTemplate mongoTemplate;
    private UserService userService;
    private CacheManager cacheManager;
    private JavaMailSender mailSender;
    private BillService billService;

    @BeforeEach
    void setUp() {
        billRepository = mock(BillRepository.class);
        billMapper = mock(BillMapper.class);
        mongoTemplate = mock(MongoTemplate.class);
        userService = mock(UserService.class);
        mailSender = mock(JavaMailSender.class);
        cacheManager = mock(CacheManager.class);

        billService = new BillService(
                billRepository,
                billMapper,
                mongoTemplate,
                userService,
                cacheManager,
                mailSender);
    }

    @Test
    void testGetBillByIdSuccess() throws ObjectNotFoundException {
        BillEntity entity = new BillEntity();
        entity.setId("bill-1");
        when(billRepository.findById("bill-1")).thenReturn(Optional.of(entity));

        BillDto dto = new BillDto("bill-1", "100", 10L, 100L, System.currentTimeMillis(), false);
        when(billMapper.toDto(entity)).thenReturn(dto);

        BillDto result = billService.getBillById("bill-1");
        assertEquals("bill-1", result.getId());
    }

    @Test
    void testGetBillByIdNotFound() {
        when(billRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundException.class, () -> billService.getBillById("non-existent"));
    }

    @Test
    void testCreateBill() {
        BillEntity entity = new BillEntity();
        entity.setId("bill-2");
        when(billRepository.save(any(BillEntity.class))).thenAnswer(invocation -> {
            BillEntity saved = invocation.getArgument(0);
            saved.setId("bill-2");
            return saved;
        });

        BillDto dto = new BillDto("bill-2", "500", 50L, 100L, 123L, false);
        when(billMapper.toDto(any(BillEntity.class))).thenReturn(dto);

        BillDto result = billService.createBill("user-1", 500L, 50L);
        assertEquals("bill-2", result.getId());
        assertFalse(result.getPayed());
        verify(billRepository).save(any(BillEntity.class));
    }

    @Test
    void testNotifyUsers() {
        BillDto billDto = new BillDto("bill-3", "user-99", 10L, 100L, 123L, false);
        UserDto userDto = new UserDto("user-99", "username99", "user@exampl.com", List.of());

        when(userService.loadUserById("user-99")).thenReturn(userDto);

        billService.notifyUsers(List.of(billDto));

        ArgumentCaptor<SimpleMailMessage> msgCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(msgCaptor.capture());
        SimpleMailMessage msg = msgCaptor.getValue();
        assertEquals("user@exampl.com", Objects.requireNonNull(msg.getTo())[0]);
        assertEquals("Bill bill-3", msg.getSubject());
        assertTrue(Objects.requireNonNull(msg.getText()).contains("please pay it: 123 BTC"));
    }
}
