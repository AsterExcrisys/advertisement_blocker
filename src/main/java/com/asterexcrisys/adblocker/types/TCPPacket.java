package com.asterexcrisys.adblocker.types;

import java.net.Socket;
import java.util.Objects;

public record TCPPacket(Socket socket, byte[] data) {

    public TCPPacket {
        Objects.requireNonNull(socket);
        Objects.requireNonNull(data);
    }

    public static TCPPacket of(Socket socket, byte[] data) {
        return new TCPPacket(socket, data);
    }

}