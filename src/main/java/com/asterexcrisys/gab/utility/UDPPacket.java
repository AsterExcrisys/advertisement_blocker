package com.asterexcrisys.gab.utility;

import java.net.InetAddress;

public record UDPPacket(InetAddress address, int port, byte[] data) {

    public static UDPPacket of(InetAddress address, int port, byte[] data) {
        return new UDPPacket(address, port, data);
    }

}