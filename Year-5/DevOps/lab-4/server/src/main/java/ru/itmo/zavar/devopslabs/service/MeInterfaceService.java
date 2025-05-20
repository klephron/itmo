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
    private final NotifyService notifyService;

    @Autowired
    public MeInterfaceService(MeInterfaceRepository meInterfaceRepository,
                              NetworkService networkService, NotifyService notifyService) {
        this.meInterfaceRepository = meInterfaceRepository;
        this.networkService = networkService;
        this.notifyService = notifyService;
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
        MeInterfaceEntity save = this.meInterfaceRepository.save(meInterface);
        notifyService.sendMessage("Create pattern entity with id %s successfully".formatted(save.getId()));
        return save;
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
        MeInterfaceEntity save = this.meInterfaceRepository.save(existing);
        notifyService.sendMessage("Update pattern entity with id %s successfully".formatted(id));
        return save;
    }

    public void delete(Long id) throws NoSuchElementException {
        MeInterfaceEntity meInterface = this.findById(id);
        NetworkEntity network = meInterface.getNetwork();
        this.networkService.freeChannels(network, meInterface.getChannelsUsed());
        this.meInterfaceRepository.delete(meInterface);
        notifyService.sendMessage("Delete pattern entity with id %s successfully".formatted(id));
    }
}
