package com.asterexcrisys.adblocker.types;

import java.net.InetAddress;

@SuppressWarnings("unused")
public record UDPPacket(InetAddress address, int port, byte[] data) {

    public static UDPPacket of(InetAddress address, int port, byte[] data) {
        return new UDPPacket(address, port, data);
    }

}