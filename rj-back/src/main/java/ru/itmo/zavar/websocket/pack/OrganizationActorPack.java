package ru.itmo.zavar.websocket.pack;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class OrganizationActorPack extends ActorPack {
    @SerializedName("name")
    private final String name;

    public OrganizationActorPack(Long id, AddressPack address, List<TaskPack> tasks, int bufferCapacity,String name) {
        super(ActorPack.Kind.ORGANIZATION, id, address, tasks, bufferCapacity);
        this.name = name;
    }

}
