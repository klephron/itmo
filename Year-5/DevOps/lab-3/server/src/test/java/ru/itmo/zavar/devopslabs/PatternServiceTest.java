package ru.itmo.zavar.devopslabs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.devopslabs.entity.*;
import ru.itmo.zavar.devopslabs.exception.NotEnoughItemsException;
import ru.itmo.zavar.devopslabs.exception.StorageCellOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.CellItemRepository;
import ru.itmo.zavar.devopslabs.repository.PatternRepository;
import ru.itmo.zavar.devopslabs.service.ImportBusService;
import ru.itmo.zavar.devopslabs.service.MeInterfaceService;
import ru.itmo.zavar.devopslabs.service.PatternService;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PatternServiceTest {

    @Mock
    private PatternRepository patternRepository;
    @Mock
    private ImportBusService importBusService;
    @Mock
    private CellItemRepository cellItemRepository;
    @Mock
    private MeInterfaceService meInterfaceService;

    @InjectMocks
    private PatternService patternService;

    private NetworkEntity network;
    private MeInterfaceEntity meInterface;
    private String outputItem;
    private PatternEntity pattern;
    private StorageCellEntity cell;
    private DriveEntity drive;

    @BeforeEach
    public void setUp() {
        network = new NetworkEntity(1L, "Network1", 100, 0);
        meInterface = new MeInterfaceEntity();
        meInterface.setNetwork(network);
        outputItem = "Steel Ingot";
        String inputItemsJson = "[{\"itemName\":\"Iron Ingot\", \"count\":3}]";
        pattern = new PatternEntity(1L, "TestPattern", inputItemsJson, outputItem, 1, meInterface);
        drive = new DriveEntity(1L, "Drive1", 0, network);
        cell = new StorageCellEntity(1L, 10, drive);
    }

    @Test
    public void testFindAll() {
        when(patternRepository.findAll()).thenReturn(List.of(pattern));
        List<PatternEntity> result = patternService.findAll();
        assertEquals(1, result.size());
        assertEquals(pattern, result.get(0));
        verify(patternRepository, times(1)).findAll();
    }

    @Test
    public void testFindById_Success() {
        when(patternRepository.findById(1L)).thenReturn(Optional.of(pattern));
        PatternEntity result = patternService.findById(1L);
        assertEquals(pattern, result);
        verify(patternRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindById_NotFound() {
        when(patternRepository.findById(1L)).thenReturn(Optional.empty());
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> patternService.findById(1L));
        assertEquals("PatternEntity with id 1 not found", exception.getMessage());
    }

    @Test
    public void testCreate() {
        MeInterfaceEntity mi = new MeInterfaceEntity();
        mi.setNetwork(network);
        PatternEntity newPattern = new PatternEntity(null, "NewPattern", "[{\"itemName\":\"Iron Ingot\", \"count\":2}]", outputItem, 2, null);
        when(meInterfaceService.findById(10L)).thenReturn(mi);
        when(patternRepository.save(newPattern)).thenReturn(new PatternEntity(2L, "NewPattern", newPattern.getInputItems(), outputItem, 2, mi));

        PatternEntity created = patternService.create(newPattern, 10L);
        assertNotNull(created.getId());
        assertEquals("NewPattern", created.getPatternName());
        assertEquals(mi, newPattern.getMeInterface());
        verify(meInterfaceService, times(1)).findById(10L);
        verify(patternRepository, times(1)).save(newPattern);
    }

    @Test
    public void testUpdate() {
        PatternEntity existingPattern = new PatternEntity(1L, "OldPattern", "[{\"itemName\":\"Iron Ingot\", \"count\":3}]", outputItem, 1, meInterface);
        PatternEntity updatedData = new PatternEntity(null, "UpdatedPattern", "[{\"itemName\":\"Iron Ingot\", \"count\":4}]", outputItem, 2, null);
        when(patternRepository.findById(1L)).thenReturn(Optional.of(existingPattern));
        when(patternRepository.save(existingPattern)).thenReturn(new PatternEntity(1L, "UpdatedPattern", updatedData.getInputItems(), outputItem, 2, meInterface));

        PatternEntity result = patternService.update(1L, updatedData);
        assertEquals("UpdatedPattern", result.getPatternName());
        assertEquals(updatedData.getInputItems(), result.getInputItems());
        assertEquals(2, result.getOutputQuantity());
        verify(patternRepository, times(1)).findById(1L);
        verify(patternRepository, times(1)).save(existingPattern);
    }

    @Test
    public void testDelete() {
        patternService.delete(1L);
        verify(patternRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testCraftItem_Success() throws StorageCellOutOfCapacityException {
        when(patternRepository.findById(1L)).thenReturn(Optional.of(pattern));
        when(importBusService.findAllCellsInNetwork(network.getId())).thenReturn(List.of(cell));
        CellItemEntity ironItem = new CellItemEntity(10L, "Iron Ingot", 6, cell);
        when(cellItemRepository.findAll()).thenReturn(List.of(ironItem));
        when(importBusService.findCellItemByName(cell.getId(), "Iron Ingot")).thenReturn(ironItem);
        when(importBusService.getTotalItemsInCell(cell.getId())).thenReturn(0);
        when(importBusService.findCellItemByName(cell.getId(), "Steel Ingot")).thenReturn(null);

        patternService.craftItem(1L, 2);

        assertEquals(0, ironItem.getQuantity());
        verify(cellItemRepository, atLeastOnce()).save(ironItem);
        verify(cellItemRepository, atLeastOnce()).save(argThat(item ->
                "Steel Ingot".equals(item.getItemName()) &&
                        item.getQuantity() == 2 &&
                        item.getStorageCell().equals(cell)
        ));
    }

    @Test
    public void testCraftItem_NotEnoughInput() {
        when(patternRepository.findById(1L)).thenReturn(Optional.of(pattern));
        when(importBusService.findAllCellsInNetwork(network.getId())).thenReturn(List.of(cell));
        CellItemEntity ironItem = new CellItemEntity(10L, "Iron Ingot", 4, cell);
        when(cellItemRepository.findAll()).thenReturn(List.of(ironItem));

        NotEnoughItemsException exception = assertThrows(NotEnoughItemsException.class,
                () -> patternService.craftItem(1L, 2));
        assertTrue(exception.getMessage().contains("Not enough Iron Ingot"));
    }

    @Test
    public void testCraftItem_StoreOutput_NoSpace() {
        when(patternRepository.findById(1L)).thenReturn(Optional.of(pattern));
        when(importBusService.findAllCellsInNetwork(network.getId())).thenReturn(List.of(cell));
        CellItemEntity ironItem = new CellItemEntity(10L, "Iron Ingot", 6, cell);
        when(cellItemRepository.findAll()).thenReturn(List.of(ironItem));
        when(importBusService.findCellItemByName(cell.getId(), "Iron Ingot")).thenReturn(ironItem);
        when(importBusService.getTotalItemsInCell(cell.getId())).thenReturn(10);

        StorageCellOutOfCapacityException exception = assertThrows(StorageCellOutOfCapacityException.class,
                () -> patternService.craftItem(1L, 2));
        assertTrue(exception.getMessage().contains("Not enough space in the network's StorageCells"));
    }
}
