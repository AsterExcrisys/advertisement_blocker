package com.asterexcrisys.adblocker.models.records;

import java.net.InetAddress;
import java.util.Objects;

@SuppressWarnings("unused")
public record UDPPacket(InetAddress address, int port, byte[] data) {

    public UDPPacket {
        Objects.requireNonNull(address);
        Objects.requireNonNull(data);
    }

    public static UDPPacket of(InetAddress address, int port, byte[] data) {
        return new UDPPacket(address, port, data);
    }

}