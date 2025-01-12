package ru.itmo.zavar.websocket.pack;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class IndividualActorPack extends ActorPack {
    @SerializedName("name")
    private final String name;
    @SerializedName("surname")
    private final String surname;
    @SerializedName("patronymic")
    private final String patronymic;

    public IndividualActorPack(Long id, AddressPack address, List<TaskPack> tasks, String name, String surname,
            String patronymic) {
        super(ActorPack.Kind.INDIVIDUAL, id, address, tasks);
        this.name = name;
        this.surname = surname;
        this.patronymic = patronymic;
    }

}
