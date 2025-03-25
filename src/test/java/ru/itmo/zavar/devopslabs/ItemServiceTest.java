package ru.itmo.zavar.devopslabs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.devopslabs.entity.ItemEntity;
import ru.itmo.zavar.devopslabs.repository.ItemRepository;
import ru.itmo.zavar.devopslabs.service.ItemService;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    public void testFindAll() {
        List<ItemEntity> items = Arrays.asList(
                new ItemEntity(1L, "Item1", 10),
                new ItemEntity(2L, "Item2", 5)
        );
        when(itemRepository.findAll()).thenReturn(items);

        List<ItemEntity> result = itemService.findAll();
        assertEquals(items, result);
        verify(itemRepository, times(1)).findAll();
    }

    @Test
    public void testFindById_Success() {
        ItemEntity item = new ItemEntity(1L, "Item1", 10);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        ItemEntity result = itemService.findById(1L);
        assertEquals(item, result);
        verify(itemRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindById_NotFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> itemService.findById(1L));
        assertEquals("ItemEntity with id 1 not found", exception.getMessage());
    }

    @Test
    public void testCreate() {
        ItemEntity item = new ItemEntity(null, "Item1", 10);
        ItemEntity savedItem = new ItemEntity(1L, "Item1", 10);
        when(itemRepository.save(item)).thenReturn(savedItem);

        ItemEntity result = itemService.create(item);
        assertEquals(savedItem, result);
        verify(itemRepository, times(1)).save(item);
    }

    @Test
    public void testUpdate() {
        ItemEntity existingItem = new ItemEntity(1L, "Item1", 10);
        ItemEntity updatedData = new ItemEntity(null, "UpdatedItem", 20);
        ItemEntity updatedItem = new ItemEntity(1L, "UpdatedItem", 20);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(existingItem));
        when(itemRepository.save(existingItem)).thenReturn(updatedItem);

        ItemEntity result = itemService.update(1L, updatedData);
        assertEquals(updatedItem.getItemName(), result.getItemName());
        assertEquals(updatedItem.getQuantity(), result.getQuantity());
        verify(itemRepository, times(1)).findById(1L);
        verify(itemRepository, times(1)).save(existingItem);
    }

    @Test
    public void testDelete() {
        itemService.delete(1L);
        verify(itemRepository, times(1)).deleteById(1L);
    }
}
