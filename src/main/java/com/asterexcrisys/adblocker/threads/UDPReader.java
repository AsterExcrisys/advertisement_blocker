package com.asterexcrisys.adblocker.threads;

import com.asterexcrisys.adblocker.types.UDPPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

@SuppressWarnings("unused")
public class UDPReader extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(UDPReader.class);

    private final DatagramSocket socket;
    private final BlockingQueue<UDPPacket> requests;

    public UDPReader(DatagramSocket socket, BlockingQueue<UDPPacket> requests) {
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
                    LOGGER.info("Succeeded to receive UDP request from {}:{}", requestPacket.address(), requestPacket.port());
                } else {
                    LOGGER.warn("Failed to receive UDP request from {}:{}", requestPacket.address(), requestPacket.port());
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to read UDP request: {}", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}