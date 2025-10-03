package com.asterexcrisys.adblocker.tasks;

import com.asterexcrisys.adblocker.models.records.TCPPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

@SuppressWarnings("unused")
public class TCPWriter extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCPWriter.class);

    private final BlockingQueue<TCPPacket> responses;

    public TCPWriter(BlockingQueue<TCPPacket> responses) {
        this.responses = Objects.requireNonNull(responses);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                TCPPacket responsePacket = responses.take();
                Socket clientSocket = responsePacket.socket();
                if (clientSocket.isClosed() || clientSocket.isOutputShutdown()) {
                    continue;
                }
                OutputStream output = clientSocket.getOutputStream();
                byte[] data = responsePacket.data();
                output.write((data.length >> 8) & 0xFF);
                output.write(data.length & 0xFF);
                output.write(data);
                output.flush();
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to send TCP response: {}", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}