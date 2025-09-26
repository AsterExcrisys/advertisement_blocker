package com.asterexcrisys.adblocker.types;

import java.net.Socket;

public record TCPPacket(Socket socket, byte[] data) {

    public static TCPPacket of(Socket socket, byte[] data) {
        return new TCPPacket(socket, data);
    }

}