package ru.itmo.zavar.bot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_ids")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatIdEntity {

    @Id
    private Long chatId;
}
