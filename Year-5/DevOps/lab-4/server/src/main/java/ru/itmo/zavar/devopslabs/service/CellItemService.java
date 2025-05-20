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
    private final NotifyService notifyService;

    @Autowired
    public CellItemService(CellItemRepository cellItemRepository,
                           StorageCellService storageCellService, NotifyService notifyService) {
        this.cellItemRepository = cellItemRepository;
        this.storageCellService = storageCellService;
        this.notifyService = notifyService;
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
        CellItemEntity save = this.cellItemRepository.save(cellItem);
        this.notifyService.sendMessage("Create storage cell entity with id %s successfully".formatted(save.getId()));
        return save;
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
        CellItemEntity save = this.cellItemRepository.save(existing);
        this.notifyService.sendMessage("Update storage cell entity with id %s successfully".formatted(id));
        return save;
    }

    public void delete(Long id) {
        this.cellItemRepository.deleteById(id);
        this.notifyService.sendMessage("Delete storage cell entity with id %s successfully".formatted(id));
    }

    //better to move it to database?
    private int getTotalItemsInCell(Long cellId) {
        List<CellItemEntity> items = this.cellItemRepository.findAll();
        return items.stream()
                .filter(i -> i.getStorageCell().getId().equals(cellId))
                .mapToInt(CellItemEntity::getQuantity)
                .sum();
    }
}
