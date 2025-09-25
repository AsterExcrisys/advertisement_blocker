package com.asterexcrisys.adblocker.threads;

import com.asterexcrisys.adblocker.types.UDPPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

@SuppressWarnings("unused")
public class Writer extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(Writer.class);

    private final DatagramSocket socket;
    private final BlockingQueue<UDPPacket> responses;

    public Writer(DatagramSocket socket, BlockingQueue<UDPPacket> responses) {
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
            LOGGER.error("Failed to send response: {}", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}