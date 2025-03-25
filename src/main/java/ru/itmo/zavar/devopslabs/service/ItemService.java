package ru.itmo.zavar.devopslabs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.devopslabs.entity.ItemEntity;
import ru.itmo.zavar.devopslabs.repository.ItemRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ItemService {

    private final ItemRepository itemRepository;

    @Autowired
    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<ItemEntity> findAll() {
        return this.itemRepository.findAll();
    }

    public ItemEntity findById(Long id) throws NoSuchElementException {
        return this.itemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("ItemEntity with id %d not found".formatted(id)));
    }

    public ItemEntity create(ItemEntity item) {
        return this.itemRepository.save(item);
    }

    public ItemEntity update(Long id, ItemEntity updatedItem) throws NoSuchElementException {
        ItemEntity item = this.findById(id);
        item.setItemName(updatedItem.getItemName());
        item.setQuantity(updatedItem.getQuantity());
        return this.itemRepository.save(item);
    }

    public void delete(Long id) {
        this.itemRepository.deleteById(id);
    }
}
