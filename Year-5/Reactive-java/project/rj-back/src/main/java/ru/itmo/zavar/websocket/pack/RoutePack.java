package ru.itmo.zavar.websocket.pack;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class RoutePack {
    @SerializedName("postoffice_ids")
    private final List<Long> postOfficeIds;
    @SerializedName("is_cyclic")
    private final Boolean isCyclic;
}
