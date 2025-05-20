package ru.itmo.zavar.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.bot.entity.ChatIdEntity;

public interface ChatIdRepository extends JpaRepository<ChatIdEntity, Long> {
}
