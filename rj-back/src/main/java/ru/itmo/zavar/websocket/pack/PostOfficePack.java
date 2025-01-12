package ru.itmo.zavar.websocket.pack;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class PostOfficePack {
    @SerializedName("id")
    private final Long id;
    @SerializedName("address")
    private final AddressPack address;
    @SerializedName("coordinates")
    private final CoordinatesPack coordinates;
    @SerializedName("terminal_mails")
    private final List<MailPack> terminalMails;
    @SerializedName("intermediate_mails")
    private final List<MailPack> intermediateMails;
    @SerializedName("terminal_mails_count")
    private final Long terminalMailsCount;
    @SerializedName("intermediate_mails_count")
    private final Long intermediateMailsCount;
    @SerializedName("transports_queued")
    private final Long transportsQueued;
    @SerializedName("transports_processed")
    private final Long transportsProcessed;

}
