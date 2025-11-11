package com.asterexcrisys.adblocker.models.records;

import com.sun.net.httpserver.HttpExchange;
import java.util.Objects;

@SuppressWarnings("unused")
public record HTTPPacket(HttpExchange transport, byte[] data) implements Packet<HttpExchange> {

    public HTTPPacket {
        Objects.requireNonNull(transport);
        Objects.requireNonNull(data);
    }

    public static HTTPPacket of(HttpExchange transport, byte[] data) {
        return new HTTPPacket(transport, data);
    }

}