package ru.itmo.zavar.devopslabs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.devopslabs.entity.NetworkEntity;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.NetworkRepository;
import ru.itmo.zavar.devopslabs.service.NetworkService;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NetworkServiceTest {

    @Mock
    private NetworkRepository networkRepository;

    @InjectMocks
    private NetworkService networkService;

    @Test
    public void testFindAll() {
        List<NetworkEntity> networks = Arrays.asList(
                new NetworkEntity(1L, "Network1", 10, 3),
                new NetworkEntity(2L, "Network2", 20, 5)
        );
        when(networkRepository.findAll()).thenReturn(networks);

        List<NetworkEntity> result = networkService.findAll();
        assertEquals(networks, result);
        verify(networkRepository).findAll();
    }

    @Test
    public void testFindById_Success() {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 10, 3);
        when(networkRepository.findById(1L)).thenReturn(Optional.of(network));

        NetworkEntity result = networkService.findById(1L);
        assertEquals(network, result);
        verify(networkRepository).findById(1L);
    }

    @Test
    public void testFindById_NotFound() {
        when(networkRepository.findById(1L)).thenReturn(Optional.empty());

        NoSuchElementException thrown = assertThrows(NoSuchElementException.class,
                () -> networkService.findById(1L));
        assertEquals("NetworkEntity with id 1 not found", thrown.getMessage());
    }

    @Test
    public void testCreate() {
        NetworkEntity network = new NetworkEntity(null, "Network1", 10, 0);
        NetworkEntity savedNetwork = new NetworkEntity(1L, "Network1", 10, 0);
        when(networkRepository.save(network)).thenReturn(savedNetwork);

        NetworkEntity result = networkService.create(network);
        assertEquals(savedNetwork, result);
        verify(networkRepository).save(network);
    }

    @Test
    public void testUpdate() {
        NetworkEntity existing = new NetworkEntity(1L, "OldName", 10, 3);
        NetworkEntity updatedData = new NetworkEntity(null, "NewName", 15, 5);
        NetworkEntity updatedEntity = new NetworkEntity(1L, "NewName", 15, 5);

        when(networkRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(networkRepository.save(existing)).thenReturn(updatedEntity);

        NetworkEntity result = networkService.update(1L, updatedData);
        assertEquals("NewName", result.getName());
        assertEquals(15, result.getChannelCapacity());
        assertEquals(5, result.getUsedChannels());

        verify(networkRepository).findById(1L);
        verify(networkRepository).save(existing);
    }

    @Test
    public void testDelete() {
        networkService.delete(1L);
        verify(networkRepository).deleteById(1L);
    }

    @Test
    public void testOccupyChannels_Success() throws NetworkOutOfCapacityException {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 10, 3);
        networkService.occupyChannels(network, 2);
        assertEquals(5, network.getUsedChannels());
        verify(networkRepository).save(network);
    }

    @Test
    public void testOccupyChannels_Failure() {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 10, 8);
        NetworkOutOfCapacityException thrown = assertThrows(NetworkOutOfCapacityException.class,
                () -> networkService.occupyChannels(network, 3));
        assertEquals("Not enough channels in the network with id 1!", thrown.getMessage());
        verify(networkRepository, never()).save(network);
    }

    @Test
    public void testFreeChannels() {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 10, 5);
        networkService.freeChannels(network, 3);
        assertEquals(2, network.getUsedChannels());
        verify(networkRepository).save(network);
    }


    @Test
    public void testFreeChannels_DoesNotGoNegative() {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 10, 2);
        networkService.freeChannels(network, 5);
        assertEquals(0, network.getUsedChannels());
        verify(networkRepository).save(network);
    }
}
