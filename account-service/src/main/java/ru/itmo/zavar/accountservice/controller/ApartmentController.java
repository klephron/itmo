package ru.itmo.zavar.accountservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.accountservice.dto.ApartmentDto;
import ru.itmo.zavar.accountservice.dto.RadiatorToApartmentDto;
import ru.itmo.zavar.accountservice.exception.ObjectNotFoundException;
import ru.itmo.zavar.accountservice.service.ApartmentService;

import java.util.List;

@RestController
@RequestMapping("/apartments")
public class ApartmentController {

    private final ApartmentService apartmentService;

    public ApartmentController(ApartmentService apartmentService) {
        this.apartmentService = apartmentService;
    }

    @PostMapping
    public ResponseEntity<ApartmentDto> createApartment(@RequestParam String userId, @RequestParam String name) {
        try {
            ApartmentDto apartment = this.apartmentService.createNewApartment(userId, name);
            return ResponseEntity.status(HttpStatus.CREATED).body(apartment);
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApartmentDto> getApartmentById(@PathVariable String id) {
        try {
            ApartmentDto apartment = this.apartmentService.getApartmentById(id);
            return ResponseEntity.ok(apartment);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ApartmentDto>> getAllApartments() {
        return ResponseEntity.ok(this.apartmentService.getAllApartments());
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<ApartmentDto> getApartmentByUserId(@PathVariable String userId) {
        try {
            ApartmentDto apartment = this.apartmentService.getApartmentByUserId(userId);
            return ResponseEntity.ok(apartment);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApartment(@PathVariable String id) {
        this.apartmentService.deleteApartmentById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{apartmentId}/radiators/{deviceId}")
    public ResponseEntity<RadiatorToApartmentDto> attachRadiatorToApartment(@PathVariable String apartmentId, @PathVariable String deviceId) {
        try {
            RadiatorToApartmentDto dto = this.apartmentService.attachRadiatorToApartment(apartmentId, deviceId);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{apartmentId}/radiators")
    public ResponseEntity<List<RadiatorToApartmentDto>> listAllRadiators(@PathVariable String apartmentId) {
        try {
            List<RadiatorToApartmentDto> list = this.apartmentService.listAllRadiators(apartmentId);
            return ResponseEntity.ok(list);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

