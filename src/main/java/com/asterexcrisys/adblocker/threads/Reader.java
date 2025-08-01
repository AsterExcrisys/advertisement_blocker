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
            byte[] buffer = new byte[512];
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                UDPPacket requestPacket = UDPPacket.of(
                        packet.getAddress(),
                        packet.getPort(),
                        Arrays.copyOf(packet.getData(), packet.getLength())
                );
                if (requests.offer(requestPacket)) {
                    System.out.println("Succeeded to receive request");
                } else {
                    System.out.println("Failed to receive request");
                }
            }
        } catch (Exception e) {
            System.out.println("Exception caught while reading: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}