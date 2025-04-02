package ru.itmo.zavar.devopslabs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.devopslabs.dto.DriveEntityDto;
import ru.itmo.zavar.devopslabs.entity.DriveEntity;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.mapper.DriveMapper;
import ru.itmo.zavar.devopslabs.service.DriveService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/drives")
public class DriveController {

    private final DriveService driveService;
    private final DriveMapper driveMapper;

    public DriveController(DriveService driveService, DriveMapper driveMapper) {
        this.driveService = driveService;
        this.driveMapper = driveMapper;
    }

    @GetMapping
    public ResponseEntity<List<DriveEntityDto>> getAllDrives() {
        List<DriveEntity> drives = driveService.findAll();
        List<DriveEntityDto> dtos = drives.stream()
                .map(driveMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriveEntityDto> getDriveById(@PathVariable Long id) {
        try {
            DriveEntity drive = driveService.findById(id);
            return ResponseEntity.ok(driveMapper.toDto(drive));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<DriveEntityDto> createDrive(@RequestBody DriveEntityDto dto,
                                                      @RequestParam Long networkId) {
        try {
            DriveEntity drive = driveMapper.fromDto(dto);
            DriveEntity created = driveService.create(drive, networkId);
            return ResponseEntity.ok(driveMapper.toDto(created));
        } catch (NoSuchElementException | NetworkOutOfCapacityException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<DriveEntityDto> updateDrive(@PathVariable Long id,
                                                      @RequestBody DriveEntityDto dto) {
        try {
            DriveEntity drive = driveMapper.fromDto(dto);
            DriveEntity updated = driveService.update(id, drive);
            return ResponseEntity.ok(driveMapper.toDto(updated));
        } catch (NoSuchElementException | NetworkOutOfCapacityException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDrive(@PathVariable Long id) {
        try {
            driveService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

