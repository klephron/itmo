package ru.itmo.zavar.devopslabs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.devopslabs.entity.*;
import ru.itmo.zavar.devopslabs.exception.NetworkOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.CellItemRepository;
import ru.itmo.zavar.devopslabs.repository.DriveRepository;
import ru.itmo.zavar.devopslabs.repository.ExportBusRepository;
import ru.itmo.zavar.devopslabs.repository.StorageCellRepository;
import ru.itmo.zavar.devopslabs.service.ExportBusService;
import ru.itmo.zavar.devopslabs.service.NetworkService;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExportBusServiceTest {

    @Mock
    private ExportBusRepository exportBusRepository;

    @Mock
    private NetworkService networkService;

    @Mock
    private DriveRepository driveRepository;

    @Mock
    private StorageCellRepository storageCellRepository;

    @Mock
    private CellItemRepository cellItemRepository;

    @InjectMocks
    private ExportBusService exportBusService;

    @Test
    public void testFindAll() {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ExportBusEntity bus1 = new ExportBusEntity(1L, "Bus1", 5, networkEntity);
        ExportBusEntity bus2 = new ExportBusEntity(2L, "Bus2", 3, networkEntity);
        List<ExportBusEntity> buses = Arrays.asList(bus1, bus2);
        when(exportBusRepository.findAll()).thenReturn(buses);

        List<ExportBusEntity> result = exportBusService.findAll();
        assertEquals(buses, result);
        verify(exportBusRepository, times(1)).findAll();
    }

    @Test
    public void testFindById_Success() {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ExportBusEntity bus = new ExportBusEntity(1L, "Bus1", 5, networkEntity);
        when(exportBusRepository.findById(1L)).thenReturn(Optional.of(bus));

        ExportBusEntity result = exportBusService.findById(1L);
        assertEquals(bus, result);
        verify(exportBusRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindById_NotFound() {
        when(exportBusRepository.findById(1L)).thenReturn(Optional.empty());
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> exportBusService.findById(1L));
        assertEquals("ExportBusEntity with id 1 not found", exception.getMessage());
    }

    @Test
    public void testCreate() throws NetworkOutOfCapacityException {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ExportBusEntity busToCreate = new ExportBusEntity(null, "Bus1", 5, null);
        ExportBusEntity savedBus = new ExportBusEntity(1L, "Bus1", 5, networkEntity);

        when(networkService.findById(1L)).thenReturn(networkEntity);
        when(exportBusRepository.save(busToCreate)).thenReturn(savedBus);

        ExportBusEntity result = exportBusService.create(busToCreate, 1L);
        assertEquals(savedBus, result);
        assertEquals(networkEntity, busToCreate.getNetwork());
        verify(networkService, times(1)).findById(1L);
        verify(networkService, times(1)).occupyChannels(networkEntity, 5);
        verify(exportBusRepository, times(1)).save(busToCreate);
    }

    @Test
    public void testUpdate() throws NetworkOutOfCapacityException {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ExportBusEntity existingBus = new ExportBusEntity(1L, "OldBus", 5, networkEntity);
        ExportBusEntity updatedData = new ExportBusEntity(null, "NewBus", 7, null);
        ExportBusEntity savedBus = new ExportBusEntity(1L, "NewBus", 7, networkEntity);

        when(exportBusRepository.findById(1L)).thenReturn(Optional.of(existingBus));
        when(exportBusRepository.save(existingBus)).thenReturn(savedBus);

        ExportBusEntity result = exportBusService.update(1L, updatedData);
        verify(networkService, times(1)).freeChannels(networkEntity, 5);
        verify(networkService, times(1)).occupyChannels(networkEntity, 7);
        assertEquals("NewBus", result.getBusName());
        assertEquals(7, result.getChannelsUsed());
        verify(exportBusRepository, times(1)).save(existingBus);
    }

    @Test
    public void testDelete() {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ExportBusEntity bus = new ExportBusEntity(1L, "Bus1", 5, networkEntity);
        when(exportBusRepository.findById(1L)).thenReturn(Optional.of(bus));

        exportBusService.delete(1L);
        verify(networkService, times(1)).freeChannels(networkEntity, 5);
        verify(exportBusRepository, times(1)).delete(bus);
    }

    @Test
    public void testExportItems_Success() {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ExportBusEntity bus = new ExportBusEntity(1L, "Bus1", 5, networkEntity);
        when(exportBusRepository.findById(1L)).thenReturn(Optional.of(bus));

        DriveEntity drive = new DriveEntity(10L, "Drive1", 5, networkEntity);
        when(driveRepository.findAll()).thenReturn(List.of(drive));

        StorageCellEntity cell = new StorageCellEntity(100L, 50, drive);
        when(storageCellRepository.findAll()).thenReturn(List.of(cell));

        CellItemEntity cellItem = new CellItemEntity(1000L, "ItemA", 5, cell);
        when(cellItemRepository.findAll()).thenReturn(List.of(cellItem));

        int exported = exportBusService.exportItems(1L, "ItemA", 3);
        assertEquals(3, exported);
        assertEquals(2, cellItem.getQuantity());
        verify(cellItemRepository, times(1)).save(cellItem);
    }

    @Test
    public void testExportItems_NoMatchingItem() {
        NetworkEntity networkEntity = new NetworkEntity(1L, "Network1", 100, 0);
        ExportBusEntity bus = new ExportBusEntity(1L, "Bus1", 5, networkEntity);
        when(exportBusRepository.findById(1L)).thenReturn(Optional.of(bus));

        DriveEntity drive = new DriveEntity(10L, "Drive1", 5, networkEntity);
        when(driveRepository.findAll()).thenReturn(List.of(drive));

        StorageCellEntity cell = new StorageCellEntity(100L, 50, drive);
        when(storageCellRepository.findAll()).thenReturn(List.of(cell));

        CellItemEntity cellItem = new CellItemEntity(1000L, "ItemA", 5, cell);
        when(cellItemRepository.findAll()).thenReturn(List.of(cellItem));

        int exported = exportBusService.exportItems(1L, "ItemB", 3);
        assertEquals(0, exported);
        verify(cellItemRepository, never()).save(any());
    }
}
