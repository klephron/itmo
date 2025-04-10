package ru.itmo.zavar.devopslabs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.devopslabs.dto.StorageCellDto;
import ru.itmo.zavar.devopslabs.entity.StorageCellEntity;
import ru.itmo.zavar.devopslabs.mapper.StorageCellMapper;
import ru.itmo.zavar.devopslabs.service.StorageCellService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/storageCells")
public class StorageCellController {

    private final StorageCellService storageCellService;
    private final StorageCellMapper storageCellMapper;

    public StorageCellController(StorageCellService storageCellService, StorageCellMapper storageCellMapper) {
        this.storageCellService = storageCellService;
        this.storageCellMapper = storageCellMapper;
    }

    @GetMapping
    public ResponseEntity<List<StorageCellDto>> getAllStorageCells() {
        List<StorageCellEntity> cells = storageCellService.findAll();
        List<StorageCellDto> dtos = cells.stream()
                .map(storageCellMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getStorageCellById(@PathVariable Long id) {
        try {
            StorageCellEntity cell = storageCellService.findById(id);
            return ResponseEntity.ok(storageCellMapper.toDto(cell));
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createStorageCell(@RequestBody StorageCellDto dto,
                                                            @RequestParam Long driveId) {
        try {
            StorageCellEntity cell = storageCellMapper.fromDto(dto);
            StorageCellEntity created = storageCellService.create(cell, driveId);
            return ResponseEntity.ok(storageCellMapper.toDto(created));
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStorageCell(@PathVariable Long id,
                                                            @RequestBody StorageCellDto dto) {
        try {
            StorageCellEntity cell = storageCellMapper.fromDto(dto);
            StorageCellEntity updated = storageCellService.update(id, cell);
            return ResponseEntity.ok(storageCellMapper.toDto(updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStorageCell(@PathVariable Long id) {
        try {
            storageCellService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
