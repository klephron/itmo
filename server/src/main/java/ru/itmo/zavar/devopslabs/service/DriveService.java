package ru.itmo.zavar.devopslabs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.devopslabs.entity.DriveEntity;
import ru.itmo.zavar.devopslabs.entity.NetworkEntity;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.DriveRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DriveService {

    private final DriveRepository driveRepository;
    private final NetworkService networkService;

    @Autowired
    public DriveService(DriveRepository driveRepository,
                        NetworkService networkService) {
        this.driveRepository = driveRepository;
        this.networkService = networkService;
    }

    public List<DriveEntity> findAll() {
        return this.driveRepository.findAll();
    }

    public DriveEntity findById(Long id) throws NoSuchElementException {
        return this.driveRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("DriveEntity with id %d not found".formatted(id)));
    }

    public DriveEntity create(DriveEntity drive, Long networkId) throws NoSuchElementException, NetworkOutOfCapacityException {
        NetworkEntity network = this.networkService.findById(networkId);

        this.networkService.occupyChannels(network, drive.getChannelsUsed());
        drive.setNetwork(network);

        return this.driveRepository.save(drive);
    }

    public DriveEntity update(Long id, DriveEntity updated) throws NoSuchElementException, NetworkOutOfCapacityException {
        DriveEntity existing = this.findById(id);
        existing.setDriveName(updated.getDriveName());

        int oldChannels = existing.getChannelsUsed();
        int newChannels = updated.getChannelsUsed();
        if (oldChannels != newChannels) {
            NetworkEntity network = existing.getNetwork();
            this.networkService.freeChannels(network, oldChannels);
            this.networkService.occupyChannels(network, newChannels);
            existing.setChannelsUsed(newChannels);
        }

        return this.driveRepository.save(existing);
    }

    public void delete(Long id) throws NoSuchElementException {
        DriveEntity drive = this.findById(id);
        this.networkService.freeChannels(drive.getNetwork(), drive.getChannelsUsed());
        this.driveRepository.delete(drive);
    }
}
