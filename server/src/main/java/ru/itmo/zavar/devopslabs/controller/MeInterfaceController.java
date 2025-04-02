package ru.itmo.zavar.devopslabs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.devopslabs.dto.MeInterfaceDto;
import ru.itmo.zavar.devopslabs.entity.MeInterfaceEntity;
import ru.itmo.zavar.devopslabs.mapper.MeInterfaceMapper;
import ru.itmo.zavar.devopslabs.service.MeInterfaceService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/interfaces")
public class MeInterfaceController {

    private final MeInterfaceService meInterfaceService;
    private final MeInterfaceMapper meInterfaceMapper;

    public MeInterfaceController(MeInterfaceService meInterfaceService, MeInterfaceMapper meInterfaceMapper) {
        this.meInterfaceService = meInterfaceService;
        this.meInterfaceMapper = meInterfaceMapper;
    }

    @GetMapping
    public ResponseEntity<List<MeInterfaceDto>> getAllInterfaces() {
        List<MeInterfaceEntity> interfaces = meInterfaceService.findAll();
        List<MeInterfaceDto> dtos = interfaces.stream()
                .map(meInterfaceMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeInterfaceDto> getInterfaceById(@PathVariable Long id) {
        try {
            MeInterfaceEntity entity = meInterfaceService.findById(id);
            return ResponseEntity.ok(meInterfaceMapper.toDto(entity));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<MeInterfaceDto> createInterface(@RequestBody MeInterfaceDto dto,
                                                          @RequestParam Long networkId) {
        try {
            MeInterfaceEntity entity = meInterfaceMapper.fromDto(dto);
            MeInterfaceEntity created = meInterfaceService.create(entity, networkId);
            return ResponseEntity.ok(meInterfaceMapper.toDto(created));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<MeInterfaceDto> updateInterface(@PathVariable Long id,
                                                          @RequestBody MeInterfaceDto dto) {
        try {
            MeInterfaceEntity entity = meInterfaceMapper.fromDto(dto);
            MeInterfaceEntity updated = meInterfaceService.update(id, entity);
            return ResponseEntity.ok(meInterfaceMapper.toDto(updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInterface(@PathVariable Long id) {
        try {
            meInterfaceService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
