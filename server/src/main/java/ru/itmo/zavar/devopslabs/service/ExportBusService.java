package ru.itmo.zavar.devopslabs.service;

import org.springframework.stereotype.Service;
import ru.itmo.zavar.devopslabs.entity.*;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.CellItemRepository;
import ru.itmo.zavar.devopslabs.repository.DriveRepository;
import ru.itmo.zavar.devopslabs.repository.ExportBusRepository;
import ru.itmo.zavar.devopslabs.repository.StorageCellRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ExportBusService {

    private final ExportBusRepository exportBusRepository;
    private final NetworkService networkService;

    private final DriveRepository driveRepository;
    private final StorageCellRepository storageCellRepository;
    private final CellItemRepository cellItemRepository;

    private final NotifyService notifyService;

    public ExportBusService(ExportBusRepository exportBusRepository,
                            NetworkService networkService,
                            DriveRepository driveRepository,
                            StorageCellRepository storageCellRepository,
                            CellItemRepository cellItemRepository, NotifyService notifyService) {
        this.exportBusRepository = exportBusRepository;
        this.networkService = networkService;
        this.driveRepository = driveRepository;
        this.storageCellRepository = storageCellRepository;
        this.cellItemRepository = cellItemRepository;
        this.notifyService = notifyService;
    }

    public List<ExportBusEntity> findAll() {
        return this.exportBusRepository.findAll();
    }

    public ExportBusEntity findById(Long id) throws NoSuchElementException {
        return this.exportBusRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ExportBusEntity with id %d not found".formatted(id)));
    }

    public ExportBusEntity create(ExportBusEntity exportBus, Long networkId) throws NoSuchElementException, NetworkOutOfCapacityException {
        NetworkEntity network = this.networkService.findById(networkId);
        this.networkService.occupyChannels(network, exportBus.getChannelsUsed());
        exportBus.setNetwork(network);
        ExportBusEntity save = this.exportBusRepository.save(exportBus);
        notifyService.sendMessage("Create export bus entity with id %s successfully".formatted(save.getId()));
        return save;
    }

    public ExportBusEntity update(Long id, ExportBusEntity updated) throws NoSuchElementException, NetworkOutOfCapacityException {
        ExportBusEntity existing = this.findById(id);
        if (existing.getChannelsUsed() != updated.getChannelsUsed()) {
            NetworkEntity net = existing.getNetwork();
            this.networkService.freeChannels(net, existing.getChannelsUsed());
            this.networkService.occupyChannels(net, updated.getChannelsUsed());
        }
        existing.setBusName(updated.getBusName());
        existing.setChannelsUsed(updated.getChannelsUsed());
        ExportBusEntity save = this.exportBusRepository.save(existing);
        notifyService.sendMessage("Update export bus entity with id %s successfully".formatted(id));
        return save;
    }

    public void delete(Long id) throws NoSuchElementException {
        ExportBusEntity bus = findById(id);
        this.networkService.freeChannels(bus.getNetwork(), bus.getChannelsUsed());
        this.exportBusRepository.delete(bus);
        notifyService.sendMessage("Delete export bus entity with id %s successfully".formatted(id));
    }

    //P.S. return count can be less than quantity
    public int exportItems(Long exportBusId, String itemName, int quantity) throws NoSuchElementException {
        ExportBusEntity bus = this.findById(exportBusId);

        NetworkEntity network = bus.getNetwork();
        List<StorageCellEntity> cells = this.findAllCellsInNetwork(network.getId());

        int remaining = quantity;
        for (StorageCellEntity cell : cells) {
            if (remaining <= 0)
                break;
            CellItemEntity ci;
            try {
                ci = this.findCellItemByName(cell.getId(), itemName);
            } catch (NoSuchElementException e) {
                continue;
            }

            int have = ci.getQuantity();
            int toRemove = Math.min(have, remaining);
            ci.setQuantity(have - toRemove);
            this.cellItemRepository.save(ci);

            remaining -= toRemove;
        }

        notifyService.sendMessage("Export items %s (%d) successfully".formatted(itemName, quantity));

        return quantity - remaining;
    }

    private List<StorageCellEntity> findAllCellsInNetwork(Long networkId) {
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

    private CellItemEntity findCellItemByName(Long cellId, String itemName) throws NoSuchElementException {
        return this.cellItemRepository.findAll().stream()
                .filter(ci -> ci.getStorageCell().getId().equals(cellId))
                .filter(ci -> ci.getItemName().equalsIgnoreCase(itemName))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("CellItemEntity with id %d not found".formatted(cellId)));
    }
}
