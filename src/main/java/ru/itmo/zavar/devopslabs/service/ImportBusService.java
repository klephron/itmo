package ru.itmo.zavar.devopslabs.service;

import org.springframework.stereotype.Service;
import ru.itmo.zavar.devopslabs.entity.*;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.exception.StorageCellOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.CellItemRepository;
import ru.itmo.zavar.devopslabs.repository.DriveRepository;
import ru.itmo.zavar.devopslabs.repository.ImportBusRepository;
import ru.itmo.zavar.devopslabs.repository.StorageCellRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ImportBusService {

    private final ImportBusRepository importBusRepository;
    private final NetworkService networkService;

    // Чтобы искать ячейки и предметы:
    private final DriveRepository driveRepository;
    private final StorageCellRepository storageCellRepository;
    private final CellItemRepository cellItemRepository;

    public ImportBusService(ImportBusRepository importBusRepository,
                            NetworkService networkService,
                            DriveRepository driveRepository,
                            StorageCellRepository storageCellRepository,
                            CellItemRepository cellItemRepository) {
        this.importBusRepository = importBusRepository;
        this.networkService = networkService;
        this.driveRepository = driveRepository;
        this.storageCellRepository = storageCellRepository;
        this.cellItemRepository = cellItemRepository;
    }

    public List<ImportBusEntity> findAll() {
        return this.importBusRepository.findAll();
    }

    public ImportBusEntity findById(Long id) {
        return this.importBusRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ImportBusEntity with id %d not found".formatted(id)));
    }

    public ImportBusEntity create(ImportBusEntity importBus, Long networkId) throws NoSuchElementException, NetworkOutOfCapacityException {
        NetworkEntity network = this.networkService.findById(networkId);
        this.networkService.occupyChannels(network, importBus.getChannelsUsed());
        importBus.setNetwork(network);
        return this.importBusRepository.save(importBus);
    }

    public ImportBusEntity update(Long id, ImportBusEntity updated) throws NoSuchElementException, NetworkOutOfCapacityException {
        ImportBusEntity existing = this.findById(id);
        if (!existing.getChannelsUsed().equals(updated.getChannelsUsed())) {
            NetworkEntity net = existing.getNetwork();
            this.networkService.freeChannels(net, existing.getChannelsUsed());
            this.networkService.occupyChannels(net, updated.getChannelsUsed());
        }
        existing.setBusName(updated.getBusName());
        existing.setChannelsUsed(updated.getChannelsUsed());
        return this.importBusRepository.save(existing);
    }

    public void delete(Long id) throws NoSuchElementException {
        ImportBusEntity bus = this.findById(id);
        this.networkService.freeChannels(bus.getNetwork(), bus.getChannelsUsed());
        this.importBusRepository.delete(bus);
    }


    public void importItems(Long importBusId, String itemName, int quantity) throws NoSuchElementException, StorageCellOutOfCapacityException {
        ImportBusEntity bus = this.findById(importBusId);

        NetworkEntity network = bus.getNetwork();
        List<StorageCellEntity> cells = this.findAllCellsInNetwork(network.getId());

        int remaining = quantity;
        for (StorageCellEntity cell : cells) {
            if (remaining <= 0)
                break;

            int usedCapacity = this.getTotalItemsInCell(cell.getId());
            int freeSpace = cell.getCapacity() - usedCapacity;
            if (freeSpace <= 0)
                continue;

            int toStore = Math.min(freeSpace, remaining);

            CellItemEntity existingItem;
            try {
                existingItem = this.findCellItemByName(cell.getId(), itemName);
                existingItem.setQuantity(existingItem.getQuantity() + toStore);
                this.cellItemRepository.save(existingItem);
            } catch (NoSuchElementException e) {
                CellItemEntity newItem = new CellItemEntity();
                newItem.setItemName(itemName);
                newItem.setQuantity(toStore);
                newItem.setStorageCell(cell);
                this.cellItemRepository.save(newItem);
            }
            remaining -= toStore;
        }

        if (remaining > 0) {
            throw new StorageCellOutOfCapacityException("Not enough space in the network's StorageCells. "
                    + (quantity - remaining) + " stored, " + remaining + " leftover.");
        }
    }

    public List<StorageCellEntity> findAllCellsInNetwork(Long networkId) {
        List<DriveEntity> drives = this.driveRepository.findAll().stream()
                .filter(d -> d.getNetwork() != null && d.getNetwork().getId().equals(networkId))
                .toList();

        List<StorageCellEntity> result = new ArrayList<>();
        for (DriveEntity drive : drives) {
            List<StorageCellEntity> cells = this.storageCellRepository.findAll().stream()
                    .filter(c -> c.getDrive() != null && c.getDrive().getId().equals(drive.getId()))
                    .toList();
            result.addAll(cells);
        }
        return result;
    }

    public int getTotalItemsInCell(Long cellId) {
        return this.cellItemRepository.findAll().stream()
                .filter(ci -> ci.getStorageCell().getId().equals(cellId))
                .mapToInt(CellItemEntity::getQuantity)
                .sum();
    }

    public CellItemEntity findCellItemByName(Long cellId, String itemName) {
        return this.cellItemRepository.findAll().stream()
                .filter(ci -> ci.getStorageCell().getId().equals(cellId))
                .filter(ci -> ci.getItemName().equalsIgnoreCase(itemName))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("CellItemEntity with name %s not found".formatted(itemName)));
    }
}
