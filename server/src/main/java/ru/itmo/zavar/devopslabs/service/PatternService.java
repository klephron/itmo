package ru.itmo.zavar.devopslabs.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.devopslabs.entity.*;
import ru.itmo.zavar.devopslabs.exception.NotEnoughItemsException;
import ru.itmo.zavar.devopslabs.exception.StorageCellOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.CellItemRepository;
import ru.itmo.zavar.devopslabs.repository.ItemRepository;
import ru.itmo.zavar.devopslabs.repository.PatternRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PatternService {

    private final PatternRepository patternRepository;
    private final ImportBusService importBusService;
    private final CellItemRepository cellItemRepository;
    private final MeInterfaceService meInterfaceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public PatternService(PatternRepository patternRepository, ImportBusService importBusService, CellItemRepository cellItemRepository,
                          MeInterfaceService meInterfaceService) {
        this.patternRepository = patternRepository;
        this.importBusService = importBusService;
        this.cellItemRepository = cellItemRepository;
        this.meInterfaceService = meInterfaceService;
    }

    public List<PatternEntity> findAll() {
        return this.patternRepository.findAll();
    }

    public PatternEntity findById(Long id) throws NoSuchElementException {
        return this.patternRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("PatternEntity with id %d not found".formatted(id)));
    }

    public PatternEntity create(PatternEntity pattern, Long meInterfaceId) throws NoSuchElementException {
        MeInterfaceEntity meInterface = this.meInterfaceService.findById(meInterfaceId);

        pattern.setMeInterface(meInterface);
        return this.patternRepository.save(pattern);
    }

    public PatternEntity update(Long id, PatternEntity updated) throws NoSuchElementException {
        PatternEntity existing = this.findById(id);
        existing.setPatternName(updated.getPatternName());
        existing.setInputItems(updated.getInputItems());
        existing.setOutputItem(updated.getOutputItem());
        existing.setOutputQuantity(updated.getOutputQuantity());
        return this.patternRepository.save(existing);
    }

    public void delete(Long id) {
        this.patternRepository.deleteById(id);
    }

    public void craftItem(Long patternId, int times) {
        PatternEntity pattern = this.findById(patternId);
        MeInterfaceEntity meInterface = pattern.getMeInterface();
        NetworkEntity network = meInterface.getNetwork();

        List<Ingredient> ingredients = this.parseInputItems(pattern.getInputItems());
        List<StorageCellEntity> cells = this.importBusService.findAllCellsInNetwork(network.getId());

        for (Ingredient ingredient : ingredients) {
            int required = ingredient.count * times;
            int available = this.getAvailableItemCount(cells, ingredient.itemName);
            if (available < required) {
                throw new NotEnoughItemsException("Not enough %s for crafting. Need %d, have %d".formatted(ingredient.itemName, required, available));
            }
        }

        for (Ingredient ingredient : ingredients) {
            int required = ingredient.count * times;
            this.removeItemsFromCells(cells, ingredient.itemName, required);
        }

        int totalOutput = pattern.getOutputQuantity() * times;
        this.storeItemsInCells(cells, pattern.getOutputItem().getItemName(), totalOutput);
    }

    private List<Ingredient> parseInputItems(String inputItemsJson) {
        try {
            return objectMapper.readValue(
                    inputItemsJson,
                    new TypeReference<>() {
                    }
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON of inputItems: " + e.getMessage(), e);
        }
    }

    private void storeItemsInCells(List<StorageCellEntity> cells, String itemName, int quantity) throws StorageCellOutOfCapacityException {
        int remaining = quantity;
        for (StorageCellEntity cell : cells) {
            if (remaining <= 0) break;
            int used = this.importBusService.getTotalItemsInCell(cell.getId());
            int free = cell.getCapacity() - used;
            int toStore = Math.min(free, remaining);
            if (toStore <= 0) continue;

            CellItemEntity existing = this.importBusService.findCellItemByName(cell.getId(), itemName);
            if (existing == null) {
                CellItemEntity newItem = new CellItemEntity();
                newItem.setStorageCell(cell);
                newItem.setItemName(itemName);
                newItem.setQuantity(toStore);
                this.cellItemRepository.save(newItem);
            } else {
                existing.setQuantity(existing.getQuantity() + toStore);
                this.cellItemRepository.save(existing);
            }
            remaining -= toStore;
        }

        if (remaining > 0) {
            throw new StorageCellOutOfCapacityException("Not enough space in the network's StorageCells for output");
        }
    }

    private void removeItemsFromCells(List<StorageCellEntity> cells, String itemName, int quantityNeeded) throws NoSuchElementException {
        int remaining = quantityNeeded;
        for (StorageCellEntity cell : cells) {
            if (remaining <= 0) break;
            CellItemEntity item = this.importBusService.findCellItemByName(cell.getId(), itemName);
            if (item == null) continue;

            int have = item.getQuantity();
            int toRemove = Math.min(have, remaining);
            item.setQuantity(have - toRemove);
            this.cellItemRepository.save(item);

            remaining -= toRemove;
        }
        if (remaining > 0) {
            throw new NoSuchElementException("Somehow not enough items to remove %s".formatted(itemName));
        }
    }

    private int getAvailableItemCount(List<StorageCellEntity> cells, String itemName) {
        List<Long> cellIds = cells.stream()
                .map(StorageCellEntity::getId)
                .toList();

        List<CellItemEntity> allItems = this.cellItemRepository.findAll().stream()
                .filter(ci -> cellIds.contains(ci.getStorageCell().getId()))
                .filter(ci -> ci.getItemName().equalsIgnoreCase(itemName))
                .toList();

        int total = 0;
        for (CellItemEntity ci : allItems) {
            total += ci.getQuantity();
        }
        return total;
    }

    public static class Ingredient {
        public String itemName;
        public int count;
    }

}
