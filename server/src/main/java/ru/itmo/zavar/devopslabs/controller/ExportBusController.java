package ru.itmo.zavar.devopslabs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.devopslabs.dto.ExportBusDto;
import ru.itmo.zavar.devopslabs.entity.ExportBusEntity;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.mapper.ExportBusMapper;
import ru.itmo.zavar.devopslabs.service.ExportBusService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/exportbuses")
public class ExportBusController {

    private final ExportBusService exportBusService;
    private final ExportBusMapper exportBusMapper;

    public ExportBusController(ExportBusService exportBusService, ExportBusMapper exportBusMapper) {
        this.exportBusService = exportBusService;
        this.exportBusMapper = exportBusMapper;
    }

    @GetMapping
    public ResponseEntity<List<ExportBusDto>> getAllExportBuses() {
        List<ExportBusEntity> buses = exportBusService.findAll();
        List<ExportBusDto> dtos = buses.stream()
                .map(exportBusMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getExportBusById(@PathVariable Long id) {
        try {
            ExportBusEntity bus = exportBusService.findById(id);
            return ResponseEntity.ok(exportBusMapper.toDto(bus));
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createExportBus(@RequestBody ExportBusDto dto,
                                                        @RequestParam Long networkId) {
        try {
            ExportBusEntity bus = exportBusMapper.fromDto(dto);
            ExportBusEntity created = exportBusService.create(bus, networkId);
            return ResponseEntity.ok(exportBusMapper.toDto(created));
        } catch (NoSuchElementException | NetworkOutOfCapacityException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExportBus(@PathVariable Long id,
                                                        @RequestBody ExportBusDto dto) {
        try {
            ExportBusEntity bus = exportBusMapper.fromDto(dto);
            ExportBusEntity updated = exportBusService.update(id, bus);
            return ResponseEntity.ok(exportBusMapper.toDto(updated));
        } catch (NoSuchElementException | NetworkOutOfCapacityException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExportBus(@PathVariable Long id) {
        try {
            exportBusService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/export")
    public ResponseEntity<?> exportItems(@PathVariable("id") Long exportBusId,
                                               @RequestParam String itemName,
                                               @RequestParam int quantity) {
        try {
            int exported = exportBusService.exportItems(exportBusId, itemName, quantity);
            return ResponseEntity.ok(exported);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
