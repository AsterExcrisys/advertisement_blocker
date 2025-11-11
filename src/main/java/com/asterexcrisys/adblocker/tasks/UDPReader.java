package com.asterexcrisys.adblocker.tasks;

import com.asterexcrisys.adblocker.models.records.UDPPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

@SuppressWarnings("unused")
public class UDPReader extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(UDPReader.class);

    private final DatagramSocket serverSocket;
    private final BlockingQueue<UDPPacket> requests;

    public UDPReader(DatagramSocket serverSocket, BlockingQueue<UDPPacket> requests) {
        this.serverSocket = Objects.requireNonNull(serverSocket);
        this.requests = Objects.requireNonNull(requests);
    }

    @Override
    public void run() {
        try {
            byte[] buffer = new byte[4096];
            while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);
                InetSocketAddress clientSocket = new InetSocketAddress(packet.getAddress(), packet.getPort());
                UDPPacket requestPacket = UDPPacket.of(
                        clientSocket,
                        Arrays.copyOf(packet.getData(), packet.getLength())
                );
                if (requests.offer(requestPacket)) {
                    LOGGER.info(
                            "Succeeded to receive UDP request from {}:{}",
                            clientSocket.getAddress().getHostAddress(),
                            clientSocket.getPort()
                    );
                } else {
                    LOGGER.warn(
                            "Failed to receive UDP request from {}:{}",
                            clientSocket.getAddress().getHostAddress(),
                            clientSocket.getPort()
                    );
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to read UDP request: {}", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}