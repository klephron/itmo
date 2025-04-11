package ru.itmo.zavar.accountservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.accountservice.dto.ApartmentDto;
import ru.itmo.zavar.accountservice.dto.RadiatorToApartmentDto;
import ru.itmo.zavar.accountservice.exception.ObjectNotFoundException;
import ru.itmo.zavar.accountservice.security.CustomUserDetails;
import ru.itmo.zavar.accountservice.service.ApartmentService;

import java.util.List;

@RestController
@RequestMapping("/apartments")
public class ApartmentController {

    private final ApartmentService apartmentService;

    public ApartmentController(ApartmentService apartmentService) {
        this.apartmentService = apartmentService;
    }

    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @PostMapping
    public ResponseEntity<?> createApartment(@RequestParam String userId, @RequestParam String name) {
        try {
            ApartmentDto apartment = this.apartmentService.createNewApartment(userId, name);
            return ResponseEntity.status(HttpStatus.CREATED).body(apartment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @GetMapping("/{id}")
    public ResponseEntity<?> getApartmentById(@PathVariable String id) {
        try {
            ApartmentDto apartment = this.apartmentService.getApartmentById(id);
            return ResponseEntity.ok(apartment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @GetMapping
    public ResponseEntity<List<ApartmentDto>> getAllApartments() {
        return ResponseEntity.ok(this.apartmentService.getAllApartments());
    }

    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<?> getApartmentByUserId(@PathVariable String userId) {
        try {
            List<ApartmentDto> apartmentsByUserId = this.apartmentService.getApartmentsByUserId(userId);
            return ResponseEntity.ok(apartmentsByUserId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyApartments(@AuthenticationPrincipal CustomUserDetails user) {
        try {
            List<ApartmentDto> apartmentsByUserId = this.apartmentService.getApartmentsByUserId(user.getId());
            return ResponseEntity.ok(apartmentsByUserId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApartment(@PathVariable String id) {
        this.apartmentService.deleteApartmentById(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @PostMapping("/{apartmentId}/radiators/{deviceId}")
    public ResponseEntity<?> attachRadiatorToApartment(@PathVariable String apartmentId, @PathVariable Long deviceId) {
        try {
            RadiatorToApartmentDto dto = this.apartmentService.attachRadiatorToApartment(apartmentId, deviceId);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PreAuthorize("hasAuthority(T(ru.itmo.zavar.accountservice.security.Roles).TSJ.toString())")
    @GetMapping("/{apartmentId}/radiators")
    public ResponseEntity<?> listAllRadiators(@PathVariable String apartmentId) {
        try {
            List<RadiatorToApartmentDto> list = this.apartmentService.listAllRadiators(apartmentId);
            return ResponseEntity.ok(list);
        } catch (ObjectNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

