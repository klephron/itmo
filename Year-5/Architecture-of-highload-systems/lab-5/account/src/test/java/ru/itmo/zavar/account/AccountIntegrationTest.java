package ru.itmo.zavar.account;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.itmo.zavar.account.dto.ApartmentDto;
import ru.itmo.zavar.account.dto.BillDto;
import ru.itmo.zavar.account.dto.PayHistoryDto;
import ru.itmo.zavar.account.entity.UserEntity;
import ru.itmo.zavar.account.exception.ObjectNotFoundException;
import ru.itmo.zavar.account.repository.UserRepository;
import ru.itmo.zavar.account.service.ApartmentService;
import ru.itmo.zavar.account.service.BillService;
import ru.itmo.zavar.account.service.PayService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class AccountIntegrationTest {

    @Container
    static MongoDBContainer mongoContainer = new MongoDBContainer("mongo:6.0");

    @Autowired
    private ApartmentService apartmentService;

    @Autowired
    private BillService billService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PayService payService;

    @BeforeAll
    static void beforeAll() {
        System.setProperty("spring.data.mongodb.uri", mongoContainer.getReplicaSetUrl());
        mongoContainer.start();
    }

    @AfterAll
    static void afterAll() {
        mongoContainer.stop();
    }

    @Test
    void testFullScenario() throws Exception {
        UserEntity user = new UserEntity();
        user.setUsername("john");
        user.setAuthorities(List.of("ROLE_USER"));
        user.setEmail("john@example.com");
        userRepository.save(user);
        String userId = user.getId();
        assertNotNull(userId, "User must have an ID after save");

        ApartmentDto apt = apartmentService.createNewApartment(userId, "MyApartment");
        assertNotNull(apt.getId(), "Apartment must have an ID after creation");

        ApartmentDto foundApt = apartmentService.getApartmentById(apt.getId());
        assertEquals("MyApartment", foundApt.getName());

        var rtaDto = apartmentService.attachRadiatorToApartment(apt.getId(), 101L);
        assertEquals(101L, rtaDto.getDeviceId());

        BillDto billDto = billService.createBill(userId, 500L, 50L);
        assertNotNull(billDto.getId());
        assertFalse(billDto.getPayed());

        List<BillDto> userBills = billService.getAllBillsByUserId(userId);
        assertEquals(1, userBills.size());
        assertEquals(500L, userBills.get(0).getHeat());

        PayHistoryDto payHistory = payService.pay(billDto.getId(), userId);

        assertNotNull(payHistory.getId(), "PayHistory entry must have an ID");
        BillDto paidBill = billService.getBillById(billDto.getId());
        assertTrue(paidBill.getPayed(), "Bill should be marked as paid");


        apartmentService.deleteApartmentById(apt.getId());
        assertThrows(ObjectNotFoundException.class, () ->
                apartmentService.getApartmentById(apt.getId()));
    }
}
