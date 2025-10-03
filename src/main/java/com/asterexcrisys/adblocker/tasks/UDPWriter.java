package com.asterexcrisys.adblocker.tasks;

import com.asterexcrisys.adblocker.models.records.UDPPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

@SuppressWarnings("unused")
public class UDPWriter extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(UDPWriter.class);

    private final DatagramSocket socket;
    private final BlockingQueue<UDPPacket> responses;

    public UDPWriter(DatagramSocket socket, BlockingQueue<UDPPacket> responses) {
        this.socket = Objects.requireNonNull(socket);
        this.responses = Objects.requireNonNull(responses);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted() && !socket.isClosed()) {
                UDPPacket responsePacket = responses.take();
                DatagramPacket packet = new DatagramPacket(
                        responsePacket.data(),
                        responsePacket.data().length,
                        responsePacket.address(),
                        responsePacket.port()
                );
                socket.send(packet);
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to send UDP response: {}", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}