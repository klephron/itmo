package ru.itmo.zavar.devopslabs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.devopslabs.dto.ExportBusEntityDto;
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
    public ResponseEntity<List<ExportBusEntityDto>> getAllExportBuses() {
        List<ExportBusEntity> buses = exportBusService.findAll();
        List<ExportBusEntityDto> dtos = buses.stream()
                .map(exportBusMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExportBusEntityDto> getExportBusById(@PathVariable Long id) {
        try {
            ExportBusEntity bus = exportBusService.findById(id);
            return ResponseEntity.ok(exportBusMapper.toDto(bus));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ExportBusEntityDto> createExportBus(@RequestBody ExportBusEntityDto dto,
                                                        @RequestParam Long networkId) {
        try {
            ExportBusEntity bus = exportBusMapper.fromDto(dto);
            ExportBusEntity created = exportBusService.create(bus, networkId);
            return ResponseEntity.ok(exportBusMapper.toDto(created));
        } catch (NoSuchElementException | NetworkOutOfCapacityException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExportBusEntityDto> updateExportBus(@PathVariable Long id,
                                                        @RequestBody ExportBusEntityDto dto) {
        try {
            ExportBusEntity bus = exportBusMapper.fromDto(dto);
            ExportBusEntity updated = exportBusService.update(id, bus);
            return ResponseEntity.ok(exportBusMapper.toDto(updated));
        } catch (NoSuchElementException | NetworkOutOfCapacityException e) {
            return ResponseEntity.badRequest().build();
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

    /**
     * Эндпоинт для экспорта товаров.
     * Принимает exportBusId, имя товара (itemName) и требуемое количество (quantity).
     * Возвращает количество успешно экспортированных товаров.
     */
    @PostMapping("/{id}/export")
    public ResponseEntity<Integer> exportItems(@PathVariable("id") Long exportBusId,
                                               @RequestParam String itemName,
                                               @RequestParam int quantity) {
        try {
            int exported = exportBusService.exportItems(exportBusId, itemName, quantity);
            return ResponseEntity.ok(exported);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            // Можно расширить обработку ошибок в зависимости от требований
            return ResponseEntity.badRequest().build();
        }
    }
}
