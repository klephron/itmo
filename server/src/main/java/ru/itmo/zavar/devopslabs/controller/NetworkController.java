package ru.itmo.zavar.devopslabs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.zavar.devopslabs.dto.NetworkDto;
import ru.itmo.zavar.devopslabs.entity.NetworkEntity;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.mapper.NetworkMapper;
import ru.itmo.zavar.devopslabs.service.NetworkService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/networks")
public class NetworkController {

    private final NetworkService networkService;
    private final NetworkMapper networkMapper;

    public NetworkController(NetworkService networkService, NetworkMapper networkMapper) {
        this.networkService = networkService;
        this.networkMapper = networkMapper;
    }

    @GetMapping
    public ResponseEntity<List<NetworkDto>> getAllNetworks() {
        List<NetworkEntity> networks = networkService.findAll();
        List<NetworkDto> dtos = networks.stream()
                .map(networkMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getNetworkById(@PathVariable Long id) {
        try {
            NetworkEntity network = networkService.findById(id);
            return ResponseEntity.ok(networkMapper.toDto(network));
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<NetworkDto> createNetwork(@RequestBody NetworkDto dto) {
        NetworkEntity entity = networkMapper.fromDto(dto);
        NetworkEntity created = networkService.create(entity);
        return ResponseEntity.ok(networkMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNetwork(@PathVariable Long id, @RequestBody NetworkDto dto) {
        try {
            NetworkEntity entity = networkMapper.fromDto(dto);
            NetworkEntity updated = networkService.update(id, entity);
            return ResponseEntity.ok(networkMapper.toDto(updated));
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNetwork(@PathVariable Long id) {
        try {
            networkService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
