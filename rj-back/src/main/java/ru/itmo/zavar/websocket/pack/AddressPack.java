package ru.itmo.zavar.websocket.pack;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class AddressPack extends PostalAddressPack {
    @SerializedName("street")
    private final String street;
    @SerializedName("house")
    private final String house;

    public AddressPack(String country, String city, String postalCode, String street, String house) {
        super(country, city, postalCode);
        this.street = street;
        this.house = house;
    }
}
