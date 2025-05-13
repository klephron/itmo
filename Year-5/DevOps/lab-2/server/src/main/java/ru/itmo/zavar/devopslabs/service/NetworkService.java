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

    @Autowired
    public NetworkService(NetworkRepository networkRepository) {
        this.networkRepository = networkRepository;
    }

    public List<NetworkEntity> findAll() {
        return this.networkRepository.findAll();
    }

    public NetworkEntity findById(Long id) throws NoSuchElementException {
        return this.networkRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("NetworkEntity with id %s not found".formatted(id)));
    }

    public NetworkEntity create(NetworkEntity network) {
        return this.networkRepository.save(network);
    }

    public NetworkEntity update(Long id, NetworkEntity updated) {
        NetworkEntity existing = this.findById(id);
        existing.setName(updated.getName());
        existing.setChannelCapacity(updated.getChannelCapacity());
        existing.setUsedChannels(updated.getUsedChannels());
        return this.networkRepository.save(existing);
    }

    public void delete(Long id) {
        this.networkRepository.deleteById(id);
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
