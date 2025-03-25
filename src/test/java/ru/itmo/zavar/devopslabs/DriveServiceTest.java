package ru.itmo.zavar.devopslabs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.devopslabs.entity.DriveEntity;
import ru.itmo.zavar.devopslabs.entity.NetworkEntity;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.DriveRepository;
import ru.itmo.zavar.devopslabs.service.DriveService;
import ru.itmo.zavar.devopslabs.service.NetworkService;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DriveServiceTest {

    @Mock
    private DriveRepository driveRepository;

    @Mock
    private NetworkService networkService;

    @InjectMocks
    private DriveService driveService;

    @Test
    public void testFindAll() {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 10, 3);
        DriveEntity drive1 = new DriveEntity(1L, "Drive1", 3, network);
        DriveEntity drive2 = new DriveEntity(2L, "Drive2", 2, network);
        List<DriveEntity> driveList = Arrays.asList(drive1, drive2);

        when(driveRepository.findAll()).thenReturn(driveList);

        List<DriveEntity> result = driveService.findAll();
        assertEquals(driveList, result);
        verify(driveRepository, times(1)).findAll();
    }

    @Test
    public void testFindById_Success() {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 10, 3);
        DriveEntity drive = new DriveEntity(1L, "Drive1", 3, network);

        when(driveRepository.findById(1L)).thenReturn(Optional.of(drive));

        DriveEntity result = driveService.findById(1L);
        assertEquals(drive, result);
        verify(driveRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindById_NotFound() {
        when(driveRepository.findById(1L)).thenReturn(Optional.empty());

        NoSuchElementException thrown = assertThrows(NoSuchElementException.class,
                () -> driveService.findById(1L));
        assertEquals("DriveEntity with id 1 not found", thrown.getMessage());
    }

    @Test
    public void testCreate_Success() throws NetworkOutOfCapacityException {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 10, 3);
        DriveEntity driveToCreate = new DriveEntity(null, "Drive1", 2, null);
        DriveEntity savedDrive = new DriveEntity(1L, "Drive1", 2, network);

        when(networkService.findById(1L)).thenReturn(network);
        when(driveRepository.save(driveToCreate)).thenReturn(savedDrive);

        DriveEntity result = driveService.create(driveToCreate, 1L);
        assertEquals(savedDrive, result);
        assertEquals(network, driveToCreate.getNetwork());
        verify(networkService, times(1)).findById(1L);
        verify(networkService, times(1)).occupyChannels(network, driveToCreate.getChannelsUsed());
        verify(driveRepository, times(1)).save(driveToCreate);
    }

    @Test
    public void testUpdate_WithChannelChange() throws NetworkOutOfCapacityException {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 10, 3);
        DriveEntity existingDrive = new DriveEntity(1L, "OldDrive", 3, network);

        DriveEntity updatedData = new DriveEntity(null, "NewDrive", 5, null);
        DriveEntity savedDrive = new DriveEntity(1L, "NewDrive", 5, network);

        when(driveRepository.findById(1L)).thenReturn(Optional.of(existingDrive));
        when(driveRepository.save(existingDrive)).thenReturn(savedDrive);

        DriveEntity result = driveService.update(1L, updatedData);
        assertEquals("NewDrive", result.getDriveName());
        assertEquals(5, result.getChannelsUsed());
        verify(networkService, times(1)).freeChannels(network, 3);
        verify(networkService, times(1)).occupyChannels(network, 5);
        verify(driveRepository, times(1)).save(existingDrive);
    }

    @Test
    public void testUpdate_WithoutChannelChange() throws NetworkOutOfCapacityException {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 10, 3);
        DriveEntity existingDrive = new DriveEntity(1L, "OldDrive", 3, network);
        DriveEntity updatedData = new DriveEntity(null, "NewDrive", 3, null);
        DriveEntity savedDrive = new DriveEntity(1L, "NewDrive", 3, network);

        when(driveRepository.findById(1L)).thenReturn(Optional.of(existingDrive));
        when(driveRepository.save(existingDrive)).thenReturn(savedDrive);

        DriveEntity result = driveService.update(1L, updatedData);
        assertEquals("NewDrive", result.getDriveName());
        assertEquals(3, result.getChannelsUsed());
        verify(networkService, never()).freeChannels(any(), anyInt());
        verify(networkService, never()).occupyChannels(any(), anyInt());
        verify(driveRepository, times(1)).save(existingDrive);
    }

    @Test
    public void testDelete() {
        NetworkEntity network = new NetworkEntity(1L, "Network1", 10, 3);
        DriveEntity drive = new DriveEntity(1L, "Drive1", 3, network);

        when(driveRepository.findById(1L)).thenReturn(Optional.of(drive));

        driveService.delete(1L);
        verify(networkService, times(1)).freeChannels(network, 3);
        verify(driveRepository, times(1)).delete(drive);
    }
}
