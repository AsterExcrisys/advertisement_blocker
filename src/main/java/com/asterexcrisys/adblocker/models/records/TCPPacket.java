package com.asterexcrisys.adblocker.models.records;

import java.net.Socket;
import java.util.Objects;

@SuppressWarnings("unused")
public record TCPPacket(Socket transport, byte[] data) implements Packet<Socket> {

    public TCPPacket {
        Objects.requireNonNull(transport);
        Objects.requireNonNull(data);
    }

    public static TCPPacket of(Socket transport, byte[] data) {
        return new TCPPacket(transport, data);
    }

}