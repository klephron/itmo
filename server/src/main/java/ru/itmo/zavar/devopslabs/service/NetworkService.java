package ru.itmo.zavar.devopslabs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.devopslabs.entity.NetworkEntity;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.NetworkRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class NetworkService {

    private final NetworkRepository networkRepository;
    private final NotifyService notifyService;

    @Autowired
    public NetworkService(NetworkRepository networkRepository, NotifyService notifyService) {
        this.networkRepository = networkRepository;
        this.notifyService = notifyService;
    }

    public List<NetworkEntity> findAll() {
        return this.networkRepository.findAll();
    }

    public NetworkEntity findById(Long id) throws NoSuchElementException {
        return this.networkRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("NetworkEntity with id %s not found".formatted(id)));
    }

    public NetworkEntity create(NetworkEntity network) {
        NetworkEntity save = this.networkRepository.save(network);
        notifyService.sendMessage("Create network entity with id %s successfully".formatted(save.getId()));
        return save;
    }

    public NetworkEntity update(Long id, NetworkEntity updated) {
        NetworkEntity existing = this.findById(id);
        existing.setName(updated.getName());
        existing.setChannelCapacity(updated.getChannelCapacity());
        existing.setUsedChannels(updated.getUsedChannels());
        NetworkEntity save = this.networkRepository.save(existing);
        notifyService.sendMessage("Update network entity with id %s successfully".formatted(id));
        return save;
    }

    public void delete(Long id) {
        this.networkRepository.deleteById(id);
        notifyService.sendMessage("Delete network entity with id %s successfully".formatted(id));
    }

    public void occupyChannels(NetworkEntity network, int channelsCount) throws NetworkOutOfCapacityException {
        int used = network.getUsedChannels();
        int capacity = network.getChannelCapacity();

        if (used + channelsCount > capacity) {
            throw new NetworkOutOfCapacityException("Not enough channels in the network with id %d!".formatted(network.getId()));
        }
        network.setUsedChannels(used + channelsCount);
        this.networkRepository.save(network);
    }

    public void freeChannels(NetworkEntity network, int channelsCount) {
        int used = network.getUsedChannels();
        network.setUsedChannels(Math.max(0, used - channelsCount)); //bruh, save from negative
        this.networkRepository.save(network);
    }
}
