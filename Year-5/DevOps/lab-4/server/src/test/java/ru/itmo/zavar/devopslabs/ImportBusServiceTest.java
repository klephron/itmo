package ru.itmo.zavar.devopslabs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.constraints.Min;
import ru.itmo.zavar.devopslabs.entity.*;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.exception.StorageCellOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.CellItemRepository;
import ru.itmo.zavar.devopslabs.repository.DriveRepository;
import ru.itmo.zavar.devopslabs.repository.ImportBusRepository;
import ru.itmo.zavar.devopslabs.repository.StorageCellRepository;
import ru.itmo.zavar.devopslabs.service.ImportBusService;
import ru.itmo.zavar.devopslabs.service.NetworkService;
import ru.itmo.zavar.devopslabs.service.NotifyService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ImportBusServiceTest {

    @Mock
    private ImportBusRepository importBusRepository;

    @Mock
    private NotifyService notifyService;

    @Mock
    private NetworkService networkService;

    @Mock
    private DriveRepository driveRepository;

    @Mock
    private StorageCellRepository storageCellRepository;

    @Mock
    private CellItemRepository cellItemRepository;

    @InjectMocks
    private ImportBusService importBusService;

    @Test
    public void testFindAll() {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ImportBusEntity bus1 = new ImportBusEntity(1L, "ImportBus1", 5, networkEntity);
        ImportBusEntity bus2 = new ImportBusEntity(2L, "ImportBus2", 3, networkEntity);
        List<ImportBusEntity> buses = List.of(bus1, bus2);
        when(importBusRepository.findAll()).thenReturn(buses);

        List<ImportBusEntity> result = importBusService.findAll();
        assertEquals(buses, result);
        verify(importBusRepository, times(1)).findAll();
    }

    @Test
    public void testFindById_Success() {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ImportBusEntity bus = new ImportBusEntity(1L, "ImportBus1", 5, networkEntity);
        when(importBusRepository.findById(1L)).thenReturn(Optional.of(bus));

        ImportBusEntity result = importBusService.findById(1L);
        assertEquals(bus, result);
        verify(importBusRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindById_NotFound() {
        when(importBusRepository.findById(1L)).thenReturn(Optional.empty());
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> importBusService.findById(1L));
        assertEquals("ImportBusEntity with id 1 not found", exception.getMessage());
    }

    @Test
    public void testCreate() throws NetworkOutOfCapacityException {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ImportBusEntity busToCreate = new ImportBusEntity(null, "ImportBus1", 5, null);
        ImportBusEntity savedBus = new ImportBusEntity(1L, "ImportBus1", 5, networkEntity);

        when(networkService.findById(1L)).thenReturn(networkEntity);
        when(importBusRepository.save(busToCreate)).thenReturn(savedBus);

        ImportBusEntity result = importBusService.create(busToCreate, 1L);
        assertEquals(savedBus, result);
        assertEquals(networkEntity, busToCreate.getNetwork());
        verify(networkService, times(1)).findById(1L);
        verify(networkService, times(1)).occupyChannels(networkEntity, 5);
        verify(importBusRepository, times(1)).save(busToCreate);
    }

    @Test
    public void testUpdate() throws NetworkOutOfCapacityException {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ImportBusEntity existingBus = new ImportBusEntity(1L, "OldImportBus", 5, networkEntity);
        ImportBusEntity updatedData = new ImportBusEntity(null, "NewImportBus", 7, null);
        ImportBusEntity savedBus = new ImportBusEntity(1L, "NewImportBus", 7, networkEntity);

        when(importBusRepository.findById(1L)).thenReturn(Optional.of(existingBus));
        when(importBusRepository.save(existingBus)).thenReturn(savedBus);

        ImportBusEntity result = importBusService.update(1L, updatedData);
        verify(networkService, times(1)).freeChannels(networkEntity, 5);
        verify(networkService, times(1)).occupyChannels(networkEntity, 7);
        assertEquals("NewImportBus", result.getBusName());
        assertEquals(7, result.getChannelsUsed());
        verify(importBusRepository, times(1)).save(existingBus);
    }

    @Test
    public void testDelete() {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ImportBusEntity bus = new ImportBusEntity(1L, "ImportBus1", 5, networkEntity);
        when(importBusRepository.findById(1L)).thenReturn(Optional.of(bus));

        importBusService.delete(1L);
        verify(networkService, times(1)).freeChannels(networkEntity, 5);
        verify(importBusRepository, times(1)).delete(bus);
    }

    @Test
    public void testImportItems_Success() throws StorageCellOutOfCapacityException {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ImportBusEntity bus = new ImportBusEntity(1L, "ImportBus1", 5, networkEntity);
        when(importBusRepository.findById(1L)).thenReturn(Optional.of(bus));

        DriveEntity drive = new DriveEntity(10L, "Drive1", 5, networkEntity);
        when(driveRepository.findAll()).thenReturn(List.of(drive));

        StorageCellEntity cell = new StorageCellEntity(100L, 10, drive);
        when(storageCellRepository.findAll()).thenReturn(List.of(cell));

        when(cellItemRepository.findAll()).thenReturn(List.of());

        assertDoesNotThrow(() -> importBusService.importItems(1L, "ItemX", 5));
        verify(cellItemRepository, times(1)).save(argThat(item ->
                item.getItemName().equals("ItemX") &&
                        item.getQuantity() == 5 &&
                        item.getStorageCell().equals(cell)
        ));
    }

    @Test
    public void testImportItems_Failure() {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ImportBusEntity bus = new ImportBusEntity(1L, "ImportBus1", 5, networkEntity);
        when(importBusRepository.findById(1L)).thenReturn(Optional.of(bus));

        DriveEntity drive = new DriveEntity(10L, "Drive1", 5, networkEntity);
        when(driveRepository.findAll()).thenReturn(List.of(drive));

        StorageCellEntity cell = new StorageCellEntity(100L, 5, drive);
        when(storageCellRepository.findAll()).thenReturn(List.of(cell));

        when(cellItemRepository.findAll()).thenReturn(List.of());

        StorageCellOutOfCapacityException exception = assertThrows(StorageCellOutOfCapacityException.class,
                () -> importBusService.importItems(1L, "ItemX", 6));
        assertTrue(exception.getMessage().contains("leftover"));
    }
}
