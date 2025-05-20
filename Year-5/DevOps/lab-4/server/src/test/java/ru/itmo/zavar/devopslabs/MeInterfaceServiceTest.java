package ru.itmo.zavar.devopslabs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.devopslabs.entity.MeInterfaceEntity;
import ru.itmo.zavar.devopslabs.entity.NetworkEntity;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.MeInterfaceRepository;
import ru.itmo.zavar.devopslabs.service.MeInterfaceService;
import ru.itmo.zavar.devopslabs.service.NetworkService;
import ru.itmo.zavar.devopslabs.service.NotifyService;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MeInterfaceServiceTest {

    @Mock
    private MeInterfaceRepository meInterfaceRepository;

    @Mock
    private NotifyService notifyService;

    @Mock
    private NetworkService networkService;

    @InjectMocks
    private MeInterfaceService meInterfaceService;

    @Test
    public void testFindAll() {
        List<MeInterfaceEntity> interfaces = Arrays.asList(
                new MeInterfaceEntity(1L, "Interface1", 5, new NetworkEntity(1L, "Network1", 100, 10)),
                new MeInterfaceEntity(2L, "Interface2", 3, new NetworkEntity(1L, "Network1", 100, 10))
        );
        when(meInterfaceRepository.findAll()).thenReturn(interfaces);

        List<MeInterfaceEntity> result = meInterfaceService.findAll();
        assertEquals(interfaces, result);
        verify(meInterfaceRepository, times(1)).findAll();
    }

    @Test
    public void testFindById_Success() {
        MeInterfaceEntity meInterface = new MeInterfaceEntity(1L, "Interface1", 5, new NetworkEntity(1L, "Network1", 100, 10));
        when(meInterfaceRepository.findById(1L)).thenReturn(Optional.of(meInterface));

        MeInterfaceEntity result = meInterfaceService.findById(1L);
        assertEquals(meInterface, result);
        verify(meInterfaceRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindById_NotFound() {
        when(meInterfaceRepository.findById(1L)).thenReturn(Optional.empty());
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> meInterfaceService.findById(1L));
        assertEquals("Interface not found", exception.getMessage());
    }

    @Test
    public void testCreate_Success() throws NetworkOutOfCapacityException {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 100, 10);
        MeInterfaceEntity meInterfaceToCreate = new MeInterfaceEntity(null, "Interface1", 5, null);
        MeInterfaceEntity savedMeInterface = new MeInterfaceEntity(1L, "Interface1", 5, network);

        when(networkService.findById(1L)).thenReturn(network);
        doNothing().when(networkService).occupyChannels(network, meInterfaceToCreate.getChannelsUsed());
        when(meInterfaceRepository.save(meInterfaceToCreate)).thenReturn(savedMeInterface);

        MeInterfaceEntity result = meInterfaceService.create(meInterfaceToCreate, 1L);
        assertEquals(savedMeInterface, result);
        assertEquals(network, meInterfaceToCreate.getNetwork());
        verify(networkService, times(1)).findById(1L);
        verify(networkService, times(1)).occupyChannels(network, 5);
        verify(meInterfaceRepository, times(1)).save(meInterfaceToCreate);
    }

    @Test
    public void testUpdate_WithChannelsChange() throws NetworkOutOfCapacityException {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 100, 10);
        MeInterfaceEntity existing = new MeInterfaceEntity(1L, "OldInterface", 5, network);
        MeInterfaceEntity updated = new MeInterfaceEntity(null, "NewInterface", 7, null);
        MeInterfaceEntity saved = new MeInterfaceEntity(1L, "NewInterface", 7, network);

        when(meInterfaceRepository.findById(1L)).thenReturn(Optional.of(existing));
        doNothing().when(networkService).freeChannels(network, 5);
        doNothing().when(networkService).occupyChannels(network, 7);
        when(meInterfaceRepository.save(existing)).thenReturn(saved);

        MeInterfaceEntity result = meInterfaceService.update(1L, updated);
        assertEquals("NewInterface", result.getInterfaceName());
        assertEquals(7, result.getChannelsUsed());
        verify(networkService, times(1)).freeChannels(network, 5);
        verify(networkService, times(1)).occupyChannels(network, 7);
        verify(meInterfaceRepository, times(1)).save(existing);
    }

    @Test
    public void testUpdate_WithoutChannelsChange() throws NetworkOutOfCapacityException {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 100, 10);
        MeInterfaceEntity existing = new MeInterfaceEntity(1L, "OldInterface", 5, network);
        MeInterfaceEntity updated = new MeInterfaceEntity(null, "NewInterface", 5, null);
        MeInterfaceEntity saved = new MeInterfaceEntity(1L, "NewInterface", 5, network);

        when(meInterfaceRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(meInterfaceRepository.save(existing)).thenReturn(saved);

        MeInterfaceEntity result = meInterfaceService.update(1L, updated);
        assertEquals("NewInterface", result.getInterfaceName());
        assertEquals(5, result.getChannelsUsed());
        verify(networkService, never()).freeChannels(any(NetworkEntity.class), anyInt());
        verify(networkService, never()).occupyChannels(any(NetworkEntity.class), anyInt());
        verify(meInterfaceRepository, times(1)).save(existing);
    }

    @Test
    public void testDelete() {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 100, 10);
        MeInterfaceEntity meInterface = new MeInterfaceEntity(1L, "Interface1", 5, network);
        when(meInterfaceRepository.findById(1L)).thenReturn(Optional.of(meInterface));
        doNothing().when(networkService).freeChannels(network, 5);
        doNothing().when(meInterfaceRepository).delete(meInterface);

        meInterfaceService.delete(1L);
        verify(networkService, times(1)).freeChannels(network, 5);
        verify(meInterfaceRepository, times(1)).delete(meInterface);
    }
}
