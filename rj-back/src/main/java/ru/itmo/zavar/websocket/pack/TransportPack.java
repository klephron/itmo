package ru.itmo.zavar.websocket.pack;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class TransportPack {
    @SerializedName("id")
    private final Long id;
    @SerializedName("capacity")
    private final Integer capacity;
    @SerializedName("mails")
    private final List<MailPack> mails;
    @SerializedName("speed")
    private final Double speed; // units in second
    @SerializedName("number")
    private final String number;
    @SerializedName("route_mail_ids")
    private final List<Long> routeMails;
    @SerializedName("loading_addresses")
    private final List<PostalAddressPack> loadingAddresses;
    @SerializedName("delivery_addresses")
    private final List<PostalAddressPack> deliveryAddresses;
    @SerializedName("coordinates")
    private final CoordinatesPack coordinates;
    @SerializedName("at_post_office_id")
    private final Long atPostOffice;

}
