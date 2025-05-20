package ru.itmo.zavar.account.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.account.dto.ApartmentDto;
import ru.itmo.zavar.account.dto.RadiatorToApartmentDto;
import ru.itmo.zavar.account.entity.ApartmentEntity;
import ru.itmo.zavar.account.entity.RadiatorToApartmentEntity;
import ru.itmo.zavar.account.exception.ObjectNotFoundException;
import ru.itmo.zavar.account.mapper.ApartmentMapper;
import ru.itmo.zavar.account.mapper.RadiatorToApartmentMapper;
import ru.itmo.zavar.account.repository.ApartmentRepository;
import ru.itmo.zavar.account.repository.RadiatorToApartmentRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j(topic = "AS-ApartmentService")
@AllArgsConstructor
@Service
public class ApartmentService {
    private final ApartmentRepository apartmentRepository;
    private final RadiatorToApartmentRepository radiatorToApartmentRepository;
    private final UserService userService;
    private final ApartmentMapper apartmentMapper;
    private final RadiatorToApartmentMapper radiatorToApartmentMapper;

    public ApartmentDto createNewApartment(String userId, String apartmentName) throws UsernameNotFoundException {
        this.userService.loadUserById(userId);
        ApartmentEntity apartmentEntity = new ApartmentEntity();
        apartmentEntity.setUserId(userId);
        apartmentEntity.setName(apartmentName);
        ApartmentEntity saved = this.apartmentRepository.save(apartmentEntity);
        log.info("Created new apartment {}", saved);
        return this.apartmentMapper.toDto(saved);
    }

    public ApartmentDto getApartmentById(String apartmentId) throws ObjectNotFoundException {
        ApartmentEntity apartmentEntity = this.apartmentRepository.findById(apartmentId).orElseThrow(() -> {
            log.info("Apartment with id {} not found", apartmentId);
            return new ObjectNotFoundException("Apartment not found");
        });
        return this.apartmentMapper.toDto(apartmentEntity);
    }

    public List<ApartmentDto> getAllApartments() {
        return this.apartmentRepository.findAll().stream().map(this.apartmentMapper::toDto).toList();
    }

    public List<ApartmentDto> getApartmentsByUserId(String userId) {
        List<ApartmentEntity> allByUserId = this.apartmentRepository.findAllByUserId(userId);
        log.info("Found {} apartment by user id {}", allByUserId.size(), userId);
        return allByUserId.stream().map(apartmentMapper::toDto).collect(Collectors.toList());
    }

    public void deleteApartmentById(String apartmentId) {
        this.apartmentRepository.deleteById(apartmentId);
        this.radiatorToApartmentRepository.deleteByApartmentId(apartmentId);
    }

    public RadiatorToApartmentDto attachRadiatorToApartment(String apartmentId, Long deviceId) throws ObjectNotFoundException {
        RadiatorToApartmentEntity radiatorToApartmentEntity = new RadiatorToApartmentEntity();
        this.getApartmentById(apartmentId);
        radiatorToApartmentEntity.setApartmentId(apartmentId);
        radiatorToApartmentEntity.setDeviceId(deviceId);
        RadiatorToApartmentEntity saved = this.radiatorToApartmentRepository.save(radiatorToApartmentEntity);
        log.info("Attached radiator to apartment {}", saved.getApartmentId());
        return this.radiatorToApartmentMapper.toDto(saved);
    }

    public List<RadiatorToApartmentDto> listAllRadiators(String apartmentId) throws ObjectNotFoundException {
        List<RadiatorToApartmentEntity> byApartmentId = this.radiatorToApartmentRepository.findByApartmentId(apartmentId);
        return byApartmentId.stream().map(radiatorToApartmentMapper::toDto).toList();
    }
}
