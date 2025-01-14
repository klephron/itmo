package ru.itmo.zavar.websocket.message;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.itmo.zavar.websocket.pack.AddressPack;
import ru.itmo.zavar.websocket.pack.CoordinatesPack;

import java.util.List;

@Getter
public class AddPostofficeMessage extends Message {
    public static final String OP = "add-postoffice";

    @SerializedName("args")
    private final Args args;

    public AddPostofficeMessage(AddressPack address, CoordinatesPack coordinates) {
        super(OP);
        args = new Args(address, coordinates);
    }

    @RequiredArgsConstructor
    @Getter
    // NOTE: if empty list -> return basic info
    // if null -> return nothing
    // if exists something
    public static class Args {
        @SerializedName("address")
        private final AddressPack address;
        @SerializedName("coordinates")
        private final CoordinatesPack coordinates;
    }
}
