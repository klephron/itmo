package ru.itmo.zavar.devopslabs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.devopslabs.dto.PatternDto;
import ru.itmo.zavar.devopslabs.entity.PatternEntity;
import ru.itmo.zavar.devopslabs.mapper.PatternMapper;
import ru.itmo.zavar.devopslabs.service.PatternService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/patterns")
public class PatternController {

    private final PatternService patternService;
    private final PatternMapper patternMapper;

    public PatternController(PatternService patternService, PatternMapper patternMapper) {
        this.patternService = patternService;
        this.patternMapper = patternMapper;
    }

    @GetMapping
    public ResponseEntity<List<PatternDto>> getAllPatterns() {
        List<PatternEntity> patterns = patternService.findAll();
        List<PatternDto> dtos = patterns.stream()
                .map(patternMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPatternById(@PathVariable Long id) {
        try {
            PatternEntity pattern = patternService.findById(id);
            return ResponseEntity.ok(patternMapper.toDto(pattern));
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createPattern(@RequestBody PatternDto dto) {
        try {
            PatternEntity entity = patternMapper.toEntity(dto);
            PatternEntity created = patternService.create(entity, dto.meInterfaceId());
            return ResponseEntity.ok(patternMapper.toDto(created));
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePattern(@PathVariable Long id,
                                                    @RequestBody PatternDto dto) {
        try {
            PatternEntity entity = patternMapper.toEntity(dto);
            PatternEntity updated = patternService.update(id, entity);
            return ResponseEntity.ok(patternMapper.toDto(updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePattern(@PathVariable Long id) {
        patternService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/craft")
    public ResponseEntity<?> craftPattern(@PathVariable Long id,
                                             @RequestParam int times) {
        try {
            patternService.craftItem(id, times);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
