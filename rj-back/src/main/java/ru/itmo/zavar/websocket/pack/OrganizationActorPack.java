package ru.itmo.zavar.websocket.pack;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.List;

@Getter
public class OrganizationActorPack extends ActorPack {
    @SerializedName("name")
    private final String name;

    public OrganizationActorPack(Long id, AddressPack address, List<TaskPack> tasks, int bufferCapacity, int bufferCurrentSize, long mailsCount, List<MailPack> buffer, String name) {
        super(ActorPack.Kind.ORGANIZATION, id, address, tasks, bufferCapacity, bufferCurrentSize, mailsCount, buffer);
        this.name = name;
    }

}
