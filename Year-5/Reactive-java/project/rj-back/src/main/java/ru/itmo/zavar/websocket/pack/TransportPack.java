package ru.itmo.zavar.websocket.pack;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class TransportPack {
    @SerializedName("id")
    private final Long id;
    @SerializedName("capacity")
    private final Integer capacity;
    @SerializedName("speed")
    private final Double speed; // units in second
    @SerializedName("ops")
    private final Double ops; // operations per second (insert/remove)
    @SerializedName("license_plate")
    private final String licensePlate;
    @SerializedName("mails")
    private final List<MailPack> mails;
    @SerializedName("mails_count")
    private final Integer mailsCount;
    @SerializedName("route")
    private final RoutePack route;
    @SerializedName("loading_addresses")
    private final List<PostalAddressPack> loadingAddresses;
    @SerializedName("delivery_addresses")
    private final List<PostalAddressPack> deliveryAddresses;
    @SerializedName("coordinates")
    private final CoordinatesPack coordinates;
    @SerializedName("at_postoffice_id")
    private final Long atPostoffice;
}
