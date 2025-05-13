package ru.itmo.zavar.devopslabs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.devopslabs.dto.CellItemDto;
import ru.itmo.zavar.devopslabs.entity.CellItemEntity;
import ru.itmo.zavar.devopslabs.exception.StorageCellOutOfCapacityException;
import ru.itmo.zavar.devopslabs.mapper.CellItemMapper;
import ru.itmo.zavar.devopslabs.service.CellItemService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cellItems")
public class CellItemController {

    private final CellItemService cellItemService;
    private final CellItemMapper cellItemMapper;

    public CellItemController(CellItemService cellItemService, CellItemMapper cellItemMapper) {
        this.cellItemService = cellItemService;
        this.cellItemMapper = cellItemMapper;
    }

    @GetMapping
    public ResponseEntity<List<CellItemDto>> getAllCellItems() {
        List<CellItemEntity> items = cellItemService.findAll();
        List<CellItemDto> dtos = items.stream()
                .map(cellItemMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCellItemById(@PathVariable Long id) {
        try {
            CellItemEntity item = cellItemService.findById(id);
            return ResponseEntity.ok(cellItemMapper.toDto(item));
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createCellItem(@RequestBody CellItemDto dto,
                                                      @RequestParam Long storageCellId) {
        try {
            CellItemEntity item = cellItemMapper.fromDto(dto);
            CellItemEntity created = cellItemService.create(item, storageCellId);
            return ResponseEntity.ok(cellItemMapper.toDto(created));
        } catch (NoSuchElementException | StorageCellOutOfCapacityException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCellItem(@PathVariable Long id,
                                                      @RequestBody CellItemDto dto) {
        try {
            CellItemEntity item = cellItemMapper.fromDto(dto);
            CellItemEntity updated = cellItemService.update(id, item);
            return ResponseEntity.ok(cellItemMapper.toDto(updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCellItem(@PathVariable Long id) {
        try {
            cellItemService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

