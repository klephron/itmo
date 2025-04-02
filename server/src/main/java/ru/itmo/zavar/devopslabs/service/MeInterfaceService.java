package ru.itmo.zavar.devopslabs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.devopslabs.entity.MeInterfaceEntity;
import ru.itmo.zavar.devopslabs.entity.NetworkEntity;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.MeInterfaceRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class MeInterfaceService {

    private final MeInterfaceRepository meInterfaceRepository;
    private final NetworkService networkService;

    @Autowired
    public MeInterfaceService(MeInterfaceRepository meInterfaceRepository,
                              NetworkService networkService) {
        this.meInterfaceRepository = meInterfaceRepository;
        this.networkService = networkService;
    }

    public List<MeInterfaceEntity> findAll() {
        return this.meInterfaceRepository.findAll();
    }

    public MeInterfaceEntity findById(Long id) throws NoSuchElementException {
        return this.meInterfaceRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Interface not found"));
    }

    public MeInterfaceEntity create(MeInterfaceEntity meInterface, Long networkId) throws NoSuchElementException, NetworkOutOfCapacityException {
        NetworkEntity network = this.networkService.findById(networkId);

        this.networkService.occupyChannels(network, meInterface.getChannelsUsed());

        meInterface.setNetwork(network);
        return this.meInterfaceRepository.save(meInterface);
    }

    public MeInterfaceEntity update(Long id, MeInterfaceEntity updated) throws NoSuchElementException, NetworkOutOfCapacityException {
        MeInterfaceEntity existing = this.findById(id);

        if (existing.getChannelsUsed() != updated.getChannelsUsed()) {
            NetworkEntity network = existing.getNetwork();
            this.networkService.freeChannels(network, existing.getChannelsUsed());
            this.networkService.occupyChannels(network, updated.getChannelsUsed());
        }

        existing.setInterfaceName(updated.getInterfaceName());
        existing.setChannelsUsed(updated.getChannelsUsed());
        return this.meInterfaceRepository.save(existing);
    }

    public void delete(Long id) throws NoSuchElementException {
        MeInterfaceEntity meInterface = this.findById(id);
        NetworkEntity network = meInterface.getNetwork();
        this.networkService.freeChannels(network, meInterface.getChannelsUsed());
        this.meInterfaceRepository.delete(meInterface);
    }
}
