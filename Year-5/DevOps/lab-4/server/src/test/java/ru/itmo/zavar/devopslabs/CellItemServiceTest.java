package ru.itmo.zavar.devopslabs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.devopslabs.entity.CellItemEntity;
import ru.itmo.zavar.devopslabs.entity.StorageCellEntity;
import ru.itmo.zavar.devopslabs.exception.StorageCellOutOfCapacityException;
import ru.itmo.zavar.devopslabs.repository.CellItemRepository;
import ru.itmo.zavar.devopslabs.service.CellItemService;
import ru.itmo.zavar.devopslabs.service.NotifyService;
import ru.itmo.zavar.devopslabs.service.StorageCellService;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CellItemServiceTest {

    @Mock
    private CellItemRepository cellItemRepository;

    @Mock
    private NotifyService notifyService;

    @Mock
    private StorageCellService storageCellService;

    @InjectMocks
    private CellItemService cellItemService;

    @Test
    public void testFindAll() {
        List<CellItemEntity> items = Arrays.asList(
                new CellItemEntity(1L, "Item1", 3, new StorageCellEntity(1L, 10, null)),
                new CellItemEntity(2L, "Item2", 2, new StorageCellEntity(1L, 10, null))
        );
        when(cellItemRepository.findAll()).thenReturn(items);

        List<CellItemEntity> result = cellItemService.findAll();
        assertEquals(items, result);
        verify(cellItemRepository, times(1)).findAll();
    }

    @Test
    public void testFindById_Success() {
        StorageCellEntity cell = new StorageCellEntity(1L, 10, null);
        CellItemEntity item = new CellItemEntity(1L, "Item1", 3, cell);
        when(cellItemRepository.findById(1L)).thenReturn(Optional.of(item));

        CellItemEntity result = cellItemService.findById(1L);
        assertEquals(item, result);
        verify(cellItemRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindById_NotFound() {
        when(cellItemRepository.findById(1L)).thenReturn(Optional.empty());
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> cellItemService.findById(1L));
        assertEquals("CellItemEntity with id 1 not found", exception.getMessage());
    }

    @Test
    public void testCreate_Success() throws StorageCellOutOfCapacityException {
        StorageCellEntity cell = new StorageCellEntity(1L, 10, null);

        List<CellItemEntity> existingItems = Arrays.asList(
                new CellItemEntity(1L, "ExistingItem", 3, cell)
        );
        when(storageCellService.findById(1L)).thenReturn(cell);
        when(cellItemRepository.findAll()).thenReturn(existingItems);


        CellItemEntity newItem = new CellItemEntity(null, "NewItem", 4, null);
        CellItemEntity savedItem = new CellItemEntity(2L, "NewItem", 4, cell);
        when(cellItemRepository.save(newItem)).thenReturn(savedItem);

        CellItemEntity result = cellItemService.create(newItem, 1L);
        assertEquals(savedItem, result);
        assertEquals(cell, newItem.getStorageCell());
        verify(storageCellService, times(1)).findById(1L);
        verify(cellItemRepository, times(1)).findAll();
        verify(cellItemRepository, times(1)).save(newItem);
    }

    @Test
    public void testCreate_FailureDueToCapacity() {
        StorageCellEntity cell = new StorageCellEntity(1L, 10, null);
        List<CellItemEntity> existingItems = Arrays.asList(
                new CellItemEntity(1L, "ExistingItem", 5, cell)
        );
        when(storageCellService.findById(1L)).thenReturn(cell);
        when(cellItemRepository.findAll()).thenReturn(existingItems);

        CellItemEntity newItem = new CellItemEntity(null, "NewItem", 6, null);

        StorageCellOutOfCapacityException exception = assertThrows(StorageCellOutOfCapacityException.class,
                () -> cellItemService.create(newItem, 1L));
        assertEquals("Not enough capacity in the storage cell! (11 vs 10)", exception.getMessage());
        verify(storageCellService, times(1)).findById(1L);
        verify(cellItemRepository, times(1)).findAll();
        verify(cellItemRepository, never()).save(any());
    }

    @Test
    public void testUpdate_Success() throws StorageCellOutOfCapacityException {
        StorageCellEntity cell = new StorageCellEntity(1L, 10, null);
        CellItemEntity existingItem = new CellItemEntity(1L, "OldName", 3, cell);
        when(cellItemRepository.findById(1L)).thenReturn(Optional.of(existingItem));
        List<CellItemEntity> allItems = Arrays.asList(
                existingItem,
                new CellItemEntity(2L, "OtherItem", 2, cell)
        );
        when(cellItemRepository.findAll()).thenReturn(allItems);

        CellItemEntity updatedItem = new CellItemEntity(null, "NewName", 4, null);
        CellItemEntity savedItem = new CellItemEntity(1L, "NewName", 4, cell);
        when(cellItemRepository.save(existingItem)).thenReturn(savedItem);

        CellItemEntity result = cellItemService.update(1L, updatedItem);
        assertEquals("NewName", result.getItemName());
        assertEquals(4, result.getQuantity());
        verify(cellItemRepository, times(1)).findById(1L);
        verify(cellItemRepository, times(1)).findAll();
        verify(cellItemRepository, times(1)).save(existingItem);
    }

    @Test
    public void testUpdate_FailureDueToCapacity() {
        StorageCellEntity cell = new StorageCellEntity(1L, 10, null);
        CellItemEntity existingItem = new CellItemEntity(1L, "OldName", 5, cell);
        when(cellItemRepository.findById(1L)).thenReturn(Optional.of(existingItem));
        when(cellItemRepository.findAll()).thenReturn(Arrays.asList(existingItem));

        CellItemEntity updatedItem = new CellItemEntity(null, "NewName", 11, null);

        StorageCellOutOfCapacityException exception = assertThrows(StorageCellOutOfCapacityException.class,
                () -> cellItemService.update(1L, updatedItem));
        assertEquals("Not enough capacity to update item quantity!(11 vs 10)", exception.getMessage());
        verify(cellItemRepository, times(1)).findById(1L);
        verify(cellItemRepository, times(1)).findAll();
        verify(cellItemRepository, never()).save(any());
    }

    @Test
    public void testDelete() {
        cellItemService.delete(1L);
        verify(cellItemRepository, times(1)).deleteById(1L);
    }
}
