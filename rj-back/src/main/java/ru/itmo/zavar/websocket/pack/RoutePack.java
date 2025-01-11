package ru.itmo.zavar.websocket.pack;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class RoutePack {
    @SerializedName("id")
    private final Long id;
    @SerializedName("post_office_ids")
    private final List<Long> postOfficeIds;
    @SerializedName("is_cyclic")
    private final Boolean isCyclic;
}
