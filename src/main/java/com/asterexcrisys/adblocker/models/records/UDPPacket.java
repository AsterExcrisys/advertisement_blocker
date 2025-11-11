package com.asterexcrisys.adblocker.models.records;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Objects;

@SuppressWarnings("unused")
public record UDPPacket(InetSocketAddress transport, byte[] data) implements Packet<InetSocketAddress> {

    public UDPPacket {
        Objects.requireNonNull(transport);
        Objects.requireNonNull(data);
    }

    public static UDPPacket of(InetSocketAddress transport, byte[] data) {
        return new UDPPacket(transport, data);
    }

    public static UDPPacket of(InetAddress address, int port, byte[] data) {
        return new UDPPacket(new InetSocketAddress(address, port), data);
    }

}