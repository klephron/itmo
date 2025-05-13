package ru.itmo.zavar.devopslabs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.devopslabs.entity.DriveEntity;
import ru.itmo.zavar.devopslabs.entity.StorageCellEntity;
import ru.itmo.zavar.devopslabs.repository.StorageCellRepository;
import ru.itmo.zavar.devopslabs.service.DriveService;
import ru.itmo.zavar.devopslabs.service.StorageCellService;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StorageCellServiceTest {

    @Mock
    private StorageCellRepository storageCellRepository;

    @Mock
    private DriveService driveService;

    @InjectMocks
    private StorageCellService storageCellService;

    @Test
    public void testFindAll() {
        List<StorageCellEntity> cells = Arrays.asList(
                new StorageCellEntity(1L, 10, null),
                new StorageCellEntity(2L, 20, null)
        );
        when(storageCellRepository.findAll()).thenReturn(cells);

        List<StorageCellEntity> result = storageCellService.findAll();
        assertEquals(cells, result);
        verify(storageCellRepository, times(1)).findAll();
    }

    @Test
    public void testFindById_Success() {
        StorageCellEntity cell = new StorageCellEntity(1L, 10, null);
        when(storageCellRepository.findById(1L)).thenReturn(Optional.of(cell));

        StorageCellEntity result = storageCellService.findById(1L);
        assertEquals(cell, result);
        verify(storageCellRepository, times(1)).findById(1L);
    }

    @Test
    public void testFindById_NotFound() {
        when(storageCellRepository.findById(1L)).thenReturn(Optional.empty());
        NoSuchElementException exception = assertThrows(NoSuchElementException.class,
                () -> storageCellService.findById(1L));
        assertEquals("StorageCellEntity not found", exception.getMessage());
    }

    @Test
    public void testCreate() {
        DriveEntity drive = new DriveEntity(1L, "Drive1", 3, null);
        StorageCellEntity cell = new StorageCellEntity(null, 10, null);
        StorageCellEntity savedCell = new StorageCellEntity(1L, 10, drive);

        when(driveService.findById(1L)).thenReturn(drive);
        when(storageCellRepository.save(cell)).thenReturn(savedCell);

        StorageCellEntity result = storageCellService.create(cell, 1L);
        assertEquals(savedCell, result);
        assertEquals(drive, cell.getDrive());
        verify(driveService, times(1)).findById(1L);
        verify(storageCellRepository, times(1)).save(cell);
    }

    @Test
    public void testUpdate() {
        StorageCellEntity existingCell = new StorageCellEntity(1L, 10, null);
        StorageCellEntity updatedData = new StorageCellEntity(null, 20, null);
        StorageCellEntity savedCell = new StorageCellEntity(1L, 20, null);

        when(storageCellRepository.findById(1L)).thenReturn(Optional.of(existingCell));
        when(storageCellRepository.save(existingCell)).thenReturn(savedCell);

        StorageCellEntity result = storageCellService.update(1L, updatedData);
        assertEquals(20, result.getCapacity());
        verify(storageCellRepository, times(1)).findById(1L);
        verify(storageCellRepository, times(1)).save(existingCell);
    }

    @Test
    public void testDelete() {
        storageCellService.delete(1L);
        verify(storageCellRepository, times(1)).deleteById(1L);
    }
}
