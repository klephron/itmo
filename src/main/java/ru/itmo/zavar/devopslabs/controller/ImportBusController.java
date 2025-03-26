package ru.itmo.zavar.devopslabs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.devopslabs.dto.ImportBusEntityDto;
import ru.itmo.zavar.devopslabs.entity.ImportBusEntity;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.mapper.ImportBusMapper;
import ru.itmo.zavar.devopslabs.service.ImportBusService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/importbuses")
public class ImportBusController {

    private final ImportBusService importBusService;
    private final ImportBusMapper importBusMapper;

    public ImportBusController(ImportBusService importBusService, ImportBusMapper importBusMapper) {
        this.importBusService = importBusService;
        this.importBusMapper = importBusMapper;
    }

    @GetMapping
    public ResponseEntity<List<ImportBusEntityDto>> getAllImportBuses() {
        List<ImportBusEntity> buses = importBusService.findAll();
        List<ImportBusEntityDto> dtos = buses.stream()
                .map(importBusMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImportBusEntityDto> getImportBusById(@PathVariable Long id) {
        try {
            ImportBusEntity bus = importBusService.findById(id);
            return ResponseEntity.ok(importBusMapper.toDto(bus));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ImportBusEntityDto> createImportBus(@RequestBody ImportBusEntityDto dto,
                                                              @RequestParam Long networkId) {
        try {
            ImportBusEntity bus = importBusMapper.fromDto(dto);
            ImportBusEntity created = importBusService.create(bus, networkId);
            return ResponseEntity.ok(importBusMapper.toDto(created));
        } catch (NoSuchElementException | NetworkOutOfCapacityException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImportBusEntityDto> updateImportBus(@PathVariable Long id,
                                                              @RequestBody ImportBusEntityDto dto) {
        try {
            ImportBusEntity bus = importBusMapper.fromDto(dto);
            ImportBusEntity updated = importBusService.update(id, bus);
            return ResponseEntity.ok(importBusMapper.toDto(updated));
        } catch (NoSuchElementException | NetworkOutOfCapacityException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImportBus(@PathVariable Long id) {
        try {
            importBusService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/import")
    public ResponseEntity<Void> importItems(@PathVariable("id") Long importBusId,
                                            @RequestParam String itemName,
                                            @RequestParam int quantity) {
        try {
            importBusService.importItems(importBusId, itemName, quantity);
            return ResponseEntity.ok().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
