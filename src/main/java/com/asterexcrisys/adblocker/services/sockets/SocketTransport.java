package com.asterexcrisys.adblocker.services.sockets;

import org.bouncycastle.tls.DatagramTransport;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Objects;

@SuppressWarnings("unused")
public class SocketTransport implements DatagramTransport {

    private final DatagramSocket serverSocket;
    private final InetAddress clientAddress;
    private final int clientPort;
    private final int mtuSize;

    public SocketTransport(InetAddress clientAddress, int clientPort, int mtuSize) throws SocketException {
        this.serverSocket = new DatagramSocket();
        this.clientAddress = Objects.requireNonNull(clientAddress);
        this.clientPort = clientPort;
        this.mtuSize = mtuSize;
    }

    @Override
    public int getReceiveLimit() {
        return mtuSize;
    }

    @Override
    public int getSendLimit() {
        return mtuSize;
    }

    @Override
    public int receive(byte[] buffer, int offset, int length, int timeout) throws java.io.IOException {
        serverSocket.setSoTimeout(timeout);
        DatagramPacket packet = new DatagramPacket(buffer, offset, length);
        try {
            serverSocket.receive(packet);
            return packet.getLength();
        } catch (Exception exception) {
            return -1;
        }
    }

    @Override
    public void send(byte[] buffer, int offset, int length) throws IOException {
        DatagramPacket packet = new DatagramPacket(buffer, offset, length, clientAddress, clientPort);
        serverSocket.send(packet);
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }

}