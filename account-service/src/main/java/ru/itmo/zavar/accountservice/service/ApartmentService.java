package ru.itmo.zavar.accountservice.service;

import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.accountservice.dto.ApartmentDto;
import ru.itmo.zavar.accountservice.dto.RadiatorToApartmentDto;
import ru.itmo.zavar.accountservice.entity.ApartmentEntity;
import ru.itmo.zavar.accountservice.entity.RadiatorToApartmentEntity;
import ru.itmo.zavar.accountservice.exception.ObjectNotFoundException;
import ru.itmo.zavar.accountservice.mapper.ApartmentMapper;
import ru.itmo.zavar.accountservice.mapper.RadiatorToApartmentMapper;
import ru.itmo.zavar.accountservice.repository.ApartmentRepository;
import ru.itmo.zavar.accountservice.repository.RadiatorToApartmentRepository;

import java.util.List;
import java.util.stream.Collectors;

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
        return this.apartmentMapper.toDto(saved);
    }

    public ApartmentDto getApartmentById(String apartmentId) throws ObjectNotFoundException {
        ApartmentEntity apartmentEntity = this.apartmentRepository.findById(apartmentId).orElseThrow(() -> new ObjectNotFoundException("Apartment not found"));
        return this.apartmentMapper.toDto(apartmentEntity);
    }

    public List<ApartmentDto> getAllApartments() {
        return this.apartmentRepository.findAll().stream().map(this.apartmentMapper::toDto).toList();
    }

    public ApartmentDto getApartmentByUserId(String userId) throws ObjectNotFoundException {
        ApartmentEntity apartmentEntity = this.apartmentRepository.findByUserId(userId).orElseThrow(() -> new ObjectNotFoundException("Apartment not found"));
        return this.apartmentMapper.toDto(apartmentEntity);
    }

    public void deleteApartmentById(String apartmentId) {
        this.apartmentRepository.deleteById(apartmentId);
    }

    public RadiatorToApartmentDto attachRadiatorToApartment(String apartmentId, Long deviceId) throws ObjectNotFoundException {
        RadiatorToApartmentEntity radiatorToApartmentEntity = new RadiatorToApartmentEntity();
        this.getApartmentById(apartmentId);
        radiatorToApartmentEntity.setApartmentId(apartmentId);
        radiatorToApartmentEntity.setDeviceId(deviceId);
        RadiatorToApartmentEntity saved = this.radiatorToApartmentRepository.save(radiatorToApartmentEntity);
        return this.radiatorToApartmentMapper.toDto(saved);
    }

    public List<RadiatorToApartmentDto> listAllRadiators(String apartmentId) throws ObjectNotFoundException {
        List<RadiatorToApartmentEntity> byApartmentId = this.radiatorToApartmentRepository.findByApartmentId(apartmentId);
        return byApartmentId.stream().map(radiatorToApartmentMapper::toDto).toList();
    }
}
