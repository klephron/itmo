package ru.itmo.zavar.devopslabs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.devopslabs.dto.CellItemEntityDto;
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
    public ResponseEntity<List<CellItemEntityDto>> getAllCellItems() {
        List<CellItemEntity> items = cellItemService.findAll();
        List<CellItemEntityDto> dtos = items.stream()
                .map(cellItemMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CellItemEntityDto> getCellItemById(@PathVariable Long id) {
        try {
            CellItemEntity item = cellItemService.findById(id);
            return ResponseEntity.ok(cellItemMapper.toDto(item));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<CellItemEntityDto> createCellItem(@RequestBody CellItemEntityDto dto,
                                                            @RequestParam Long storageCellId) {
        try {
            CellItemEntity item = cellItemMapper.fromDto(dto);
            CellItemEntity created = cellItemService.create(item, storageCellId);
            return ResponseEntity.ok(cellItemMapper.toDto(created));
        } catch (NoSuchElementException | StorageCellOutOfCapacityException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CellItemEntityDto> updateCellItem(@PathVariable Long id,
                                                            @RequestBody CellItemEntityDto dto) {
        try {
            CellItemEntity item = cellItemMapper.fromDto(dto);
            CellItemEntity updated = cellItemService.update(id, item);
            return ResponseEntity.ok(cellItemMapper.toDto(updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCellItem(@PathVariable Long id) {
        try {
            cellItemService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

