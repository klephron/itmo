package ru.itmo.zavar.websocket.pack;

import com.google.gson.annotations.SerializedName;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PostalAddressPack {
    @SerializedName("country")
    private final String country;
    @SerializedName("city")
    private final String city;
    @SerializedName("postal_code")
    private final String postalCode;
}
