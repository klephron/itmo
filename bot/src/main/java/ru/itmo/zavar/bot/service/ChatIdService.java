package ru.itmo.zavar.bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.bot.entity.ChatIdEntity;
import ru.itmo.zavar.bot.repository.ChatIdRepository;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatIdService {

    private final ChatIdRepository repository;

    public void saveChatId(Long chatId) {
        if (!repository.existsById(chatId)) {
            repository.save(new ChatIdEntity(chatId));
        }
    }

    public Set<Long> getAllChatIds() {
        return repository.findAll()
                .stream()
                .map(ChatIdEntity::getChatId)
                .collect(Collectors.toSet());
    }
}