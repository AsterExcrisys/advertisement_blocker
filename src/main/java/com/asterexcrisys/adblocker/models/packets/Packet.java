package com.asterexcrisys.adblocker.models.packets;

@SuppressWarnings("unused")
public sealed interface Packet<T> permits UDPPacket, TCPPacket, HTTPPacket {

    T transport();

    byte[] data();

}