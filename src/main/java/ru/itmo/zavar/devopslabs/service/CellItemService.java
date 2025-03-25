package ru.itmo.zavar.devopslabs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.devopslabs.entity.CellItemEntity;
import ru.itmo.zavar.devopslabs.entity.StorageCellEntity;
import ru.itmo.zavar.devopslabs.exception.StorageCellOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.CellItemRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CellItemService {

    private final CellItemRepository cellItemRepository;
    private final StorageCellService storageCellService;

    @Autowired
    public CellItemService(CellItemRepository cellItemRepository,
                           StorageCellService storageCellService) {
        this.cellItemRepository = cellItemRepository;
        this.storageCellService = storageCellService;
    }

    public List<CellItemEntity> findAll() {
        return this.cellItemRepository.findAll();
    }

    public CellItemEntity findById(Long id) throws NoSuchElementException {
        return this.cellItemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("CellItemEntity with id %d not found".formatted(id)));
    }

    public CellItemEntity create(CellItemEntity cellItem, Long storageCellId) throws NoSuchElementException, StorageCellOutOfCapacityException {
        StorageCellEntity cell = this.storageCellService.findById(storageCellId);

        int currentCount = this.getTotalItemsInCell(cell.getId());
        int newTotal = currentCount + cellItem.getQuantity();

        if (newTotal > cell.getCapacity()) {
            throw new StorageCellOutOfCapacityException("Not enough capacity in the storage cell! (%d vs %d)".formatted(newTotal, cell.getCapacity()));
        }

        cellItem.setStorageCell(cell);
        return this.cellItemRepository.save(cellItem);
    }

    public CellItemEntity update(Long id, CellItemEntity updated) throws NoSuchElementException, StorageCellOutOfCapacityException {
        CellItemEntity existing = this.findById(id);

        StorageCellEntity cell = existing.getStorageCell();
        int currentCount = this.getTotalItemsInCell(cell.getId());

        int newTotal = (currentCount - existing.getQuantity()) + updated.getQuantity();

        if (newTotal > cell.getCapacity()) {
            throw new StorageCellOutOfCapacityException("Not enough capacity to update item quantity!(%d vs %d)".formatted(newTotal, cell.getCapacity()));
        }

        existing.setItemName(updated.getItemName());
        existing.setQuantity(updated.getQuantity());
        return this.cellItemRepository.save(existing);
    }

    public void delete(Long id) {
        this.cellItemRepository.deleteById(id);
    }

    //wat
    private int getTotalItemsInCell(Long cellId) {
        List<CellItemEntity> items = this.cellItemRepository.findAll();
        return items.stream()
                .filter(i -> i.getStorageCell().getId().equals(cellId))
                .mapToInt(CellItemEntity::getQuantity)
                .sum();
    }
}
