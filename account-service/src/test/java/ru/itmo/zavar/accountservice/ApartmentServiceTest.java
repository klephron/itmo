package ru.itmo.zavar.accountservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.itmo.zavar.accountservice.dto.ApartmentDto;
import ru.itmo.zavar.accountservice.dto.RadiatorToApartmentDto;
import ru.itmo.zavar.accountservice.entity.ApartmentEntity;
import ru.itmo.zavar.accountservice.entity.RadiatorToApartmentEntity;
import ru.itmo.zavar.accountservice.exception.ObjectNotFoundException;
import ru.itmo.zavar.accountservice.mapper.ApartmentMapper;
import ru.itmo.zavar.accountservice.mapper.RadiatorToApartmentMapper;
import ru.itmo.zavar.accountservice.repository.ApartmentRepository;
import ru.itmo.zavar.accountservice.repository.RadiatorToApartmentRepository;
import ru.itmo.zavar.accountservice.service.ApartmentService;
import ru.itmo.zavar.accountservice.service.UserService;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("unit")
public class ApartmentServiceTest {

    private ApartmentRepository apartmentRepository;
    private RadiatorToApartmentRepository radiatorToApartmentRepository;
    private UserService userService;
    private ApartmentMapper apartmentMapper;
    private RadiatorToApartmentMapper radiatorToApartmentMapper;
    private ApartmentService apartmentService;

    @BeforeEach
    void setUp() {
        apartmentRepository = mock(ApartmentRepository.class);
        radiatorToApartmentRepository = mock(RadiatorToApartmentRepository.class);
        userService = mock(UserService.class);
        apartmentMapper = mock(ApartmentMapper.class);
        radiatorToApartmentMapper = mock(RadiatorToApartmentMapper.class);

        apartmentService = new ApartmentService(
                apartmentRepository,
                radiatorToApartmentRepository,
                userService,
                apartmentMapper,
                radiatorToApartmentMapper
        );
    }

    @Test
    void testCreateNewApartment() {
        String userId = "123";
        String apartmentName = "MyApartment";

        doReturn(null).when(userService).loadUserById(userId);

        ApartmentEntity savedEntity = new ApartmentEntity();
        savedEntity.setId("apt-1");
        savedEntity.setUserId(userId);
        savedEntity.setName(apartmentName);

        when(apartmentRepository.save(any())).thenReturn(savedEntity);

        ApartmentDto mappedDto = new ApartmentDto("apt-1", userId, apartmentName);
        when(apartmentMapper.toDto(any())).thenReturn(mappedDto);

        ApartmentDto result = apartmentService.createNewApartment(userId, apartmentName);

        assertNotNull(result);
        assertEquals("apt-1", result.getId());
        verify(apartmentRepository).save(any(ApartmentEntity.class));
        verify(userService).loadUserById(userId);
    }

    @Test
    void testGetApartmentByIdSuccess() throws ObjectNotFoundException {
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId("apt-1");
        apt.setName("Some Apt");
        when(apartmentRepository.findById("apt-1")).thenReturn(Optional.of(apt));

        ApartmentDto mappedDto = new ApartmentDto("apt-1", "user-1", "Some Apt");
        when(apartmentMapper.toDto(apt)).thenReturn(mappedDto);

        ApartmentDto result = apartmentService.getApartmentById("apt-1");
        assertNotNull(result);
        assertEquals("apt-1", result.getId());
    }

    @Test
    void testGetApartmentByIdNotFound() {
        when(apartmentRepository.findById("non-existent")).thenReturn(Optional.empty());

        assertThrows(ObjectNotFoundException.class, () ->
                apartmentService.getApartmentById("non-existent"));
    }

    @Test
    void testDeleteApartmentById() {
        apartmentService.deleteApartmentById("apt-1");
        verify(apartmentRepository).deleteById("apt-1");
        verify(radiatorToApartmentRepository).deleteByApartmentId("apt-1");
    }

    @Test
    void testAttachRadiatorToApartment() throws ObjectNotFoundException {
        ApartmentEntity apt = new ApartmentEntity();
        apt.setId("apt-1");
        when(apartmentRepository.findById("apt-1")).thenReturn(Optional.of(apt));

        RadiatorToApartmentEntity savedEntity = new RadiatorToApartmentEntity();
        savedEntity.setApartmentId("apt-1");
        savedEntity.setDeviceId(101L);
        when(radiatorToApartmentRepository.save(any())).thenReturn(savedEntity);

        RadiatorToApartmentDto mappedDto = new RadiatorToApartmentDto("apt-1", 101L);
        when(radiatorToApartmentMapper.toDto(any())).thenReturn(mappedDto);

        RadiatorToApartmentDto result = apartmentService.attachRadiatorToApartment("apt-1", 101L);
        assertNotNull(result);
        verify(apartmentRepository).findById("apt-1");
        verify(radiatorToApartmentRepository).save(any(RadiatorToApartmentEntity.class));
    }
}
