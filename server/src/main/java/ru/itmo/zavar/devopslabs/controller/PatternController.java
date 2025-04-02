package ru.itmo.zavar.devopslabs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.devopslabs.dto.PatternEntityDto;
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
    public ResponseEntity<List<PatternEntityDto>> getAllPatterns() {
        List<PatternEntity> patterns = patternService.findAll();
        List<PatternEntityDto> dtos = patterns.stream()
                .map(patternMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatternEntityDto> getPatternById(@PathVariable Long id) {
        try {
            PatternEntity pattern = patternService.findById(id);
            return ResponseEntity.ok(patternMapper.toDto(pattern));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<PatternEntityDto> createPattern(@RequestBody PatternEntityDto dto) {
        try {
            PatternEntity entity = patternMapper.fromDto(dto);
            PatternEntity created = patternService.create(entity, dto.meInterfaceId());
            return ResponseEntity.ok(patternMapper.toDto(created));
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatternEntityDto> updatePattern(@PathVariable Long id,
                                                          @RequestBody PatternEntityDto dto) {
        try {
            PatternEntity entity = patternMapper.fromDto(dto);
            PatternEntity updated = patternService.update(id, entity);
            return ResponseEntity.ok(patternMapper.toDto(updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePattern(@PathVariable Long id) {
        patternService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/craft")
    public ResponseEntity<Void> craftPattern(@PathVariable Long id,
                                             @RequestParam int times) {
        try {
            patternService.craftItem(id, times);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
