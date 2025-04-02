package ru.itmo.zavar.devopslabs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.devopslabs.dto.ItemDto;
import ru.itmo.zavar.devopslabs.entity.ItemEntity;
import ru.itmo.zavar.devopslabs.mapper.ItemMapper;
import ru.itmo.zavar.devopslabs.service.ItemService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;
    private final ItemMapper itemMapper;

    public ItemController(ItemService itemService, ItemMapper itemMapper) {
        this.itemService = itemService;
        this.itemMapper = itemMapper;
    }

    @GetMapping
    public ResponseEntity<List<ItemDto>> getAllItems() {
        List<ItemEntity> items = itemService.findAll();
        List<ItemDto> dtos = items.stream()
                .map(itemMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDto> getItemById(@PathVariable Long id) {
        try {
            ItemEntity item = itemService.findById(id);
            return ResponseEntity.ok(itemMapper.toDto(item));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<ItemDto> createItem(@RequestBody ItemDto dto) {
        ItemEntity item = itemMapper.fromDto(dto);
        ItemEntity created = itemService.create(item);
        return ResponseEntity.ok(itemMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemDto> updateItem(@PathVariable Long id, @RequestBody ItemDto dto) {
        try {
            ItemEntity item = itemMapper.fromDto(dto);
            ItemEntity updated = itemService.update(id, item);
            return ResponseEntity.ok(itemMapper.toDto(updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        try {
            itemService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
