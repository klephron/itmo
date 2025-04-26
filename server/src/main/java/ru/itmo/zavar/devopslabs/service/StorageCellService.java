package ru.itmo.zavar.devopslabs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.devopslabs.entity.DriveEntity;
import ru.itmo.zavar.devopslabs.entity.StorageCellEntity;
import ru.itmo.zavar.devopslabs.repository.StorageCellRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class StorageCellService {

    private final StorageCellRepository storageCellRepository;
    private final DriveService driveService;
    private final NotifyService notifyService;

    @Autowired
    public StorageCellService(StorageCellRepository storageCellRepository,
                              DriveService driveService, NotifyService notifyService) {
        this.storageCellRepository = storageCellRepository;
        this.driveService = driveService;
        this.notifyService = notifyService;
    }

    public List<StorageCellEntity> findAll() {
        return this.storageCellRepository.findAll();
    }

    public StorageCellEntity findById(Long id) throws NoSuchElementException {
        return this.storageCellRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("StorageCellEntity not found"));
    }

    public StorageCellEntity create(StorageCellEntity cell, Long driveId) throws NoSuchElementException {
        DriveEntity drive = this.driveService.findById(driveId);
        cell.setDrive(drive);
        StorageCellEntity save = this.storageCellRepository.save(cell);
        notifyService.sendMessage("Create storage cell entity with id %s successfully".formatted(save.getId()));
        return save;
    }

    public StorageCellEntity update(Long id, StorageCellEntity updated) throws NoSuchElementException {
        StorageCellEntity existing = this.findById(id);
        existing.setCapacity(updated.getCapacity());
        StorageCellEntity save = this.storageCellRepository.save(existing);
        notifyService.sendMessage("Update storage cell entity with id %s successfully".formatted(id));
        return save;
    }

    public void delete(Long id) {
        storageCellRepository.deleteById(id);
        notifyService.sendMessage("Delete storage cell entity with id %s successfully".formatted(id));
    }
}
