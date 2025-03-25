package ru.itmo.zavar.devopslabs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.devopslabs.entity.MeInterfaceEntity;
import ru.itmo.zavar.devopslabs.entity.PatternEntity;
import ru.itmo.zavar.devopslabs.repository.PatternRepository;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PatternService {

    private final PatternRepository patternRepository;
    private final MeInterfaceService meInterfaceService;

    @Autowired
    public PatternService(PatternRepository patternRepository,
                          MeInterfaceService meInterfaceService) {
        this.patternRepository = patternRepository;
        this.meInterfaceService = meInterfaceService;
    }

    public List<PatternEntity> findAll() {
        return this.patternRepository.findAll();
    }

    public PatternEntity findById(Long id) throws NoSuchElementException {
        return this.patternRepository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("PatternEntity with id %d not found".formatted(id)));
    }

    public PatternEntity create(PatternEntity pattern, Long meInterfaceId) throws NoSuchElementException {
        MeInterfaceEntity meInterface = this.meInterfaceService.findById(meInterfaceId);

        pattern.setMeInterface(meInterface);
        return this.patternRepository.save(pattern);
    }

    public PatternEntity update(Long id, PatternEntity updated) throws NoSuchElementException {
        PatternEntity existing = this.findById(id);
        existing.setPatternName(updated.getPatternName());
        existing.setInputItems(updated.getInputItems());
        existing.setOutputItemName(updated.getOutputItemName());
        existing.setOutputQuantity(updated.getOutputQuantity());
        return this.patternRepository.save(existing);
    }

    public void delete(Long id) {
        this.patternRepository.deleteById(id);
    }
}
