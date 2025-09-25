package com.asterexcrisys.adblocker.threads;

import com.asterexcrisys.adblocker.types.UDPPacket;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

@SuppressWarnings("unused")
public class Reader extends Thread {

    private final DatagramSocket socket;
    private final BlockingQueue<UDPPacket> requests;

    public Reader(DatagramSocket socket, BlockingQueue<UDPPacket> requests) {
        this.socket = Objects.requireNonNull(socket);
        this.requests = Objects.requireNonNull(requests);
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[4096];
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                UDPPacket requestPacket = UDPPacket.of(
                        packet.getAddress(),
                        packet.getPort(),
                        Arrays.copyOf(packet.getData(), packet.getLength())
                );
                if (requests.offer(requestPacket)) {
                    System.out.printf("Information: succeeded to receive request from %s:%s\n", requestPacket.address(), requestPacket.port());
                } else {
                    System.out.printf("Warning: failed to receive request from %s:%s\n", requestPacket.address(), requestPacket.port());
                }
            }
        } catch (Exception exception) {
            System.err.printf("Error: %s\n", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}