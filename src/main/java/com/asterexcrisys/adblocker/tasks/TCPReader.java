package com.asterexcrisys.adblocker.tasks;

import com.asterexcrisys.adblocker.models.records.TCPPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class TCPReader extends Thread {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCPReader.class);

    private final ServerSocket serverSocket;
    private final BlockingQueue<TCPPacket> requests;

    public TCPReader(ServerSocket serverSocket, BlockingQueue<TCPPacket> requests) {
        this.serverSocket = Objects.requireNonNull(serverSocket);
        this.requests = Objects.requireNonNull(requests);
    }

    @Override
    public void run() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            while (!Thread.currentThread().isInterrupted() && !serverSocket.isClosed()) {
                final Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleRequest(clientSocket));
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to accept TCP connection: {}", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void handleRequest(Socket clientSocket) {
        try (clientSocket) {
            byte[] buffer = new byte[4096];
            while (!Thread.currentThread().isInterrupted() && !clientSocket.isClosed() && !clientSocket.isInputShutdown()) {
                int length = handlePacket(
                        clientSocket.getInputStream(),
                        clientSocket.getOutputStream(),
                        buffer
                );
                if (length == -1) {
                    break;
                }
                TCPPacket requestPacket = TCPPacket.of(
                        clientSocket,
                        Arrays.copyOf(buffer, length)
                );
                if (requests.offer(requestPacket)) {
                    LOGGER.info(
                            "Succeeded to receive TCP request from {}:{}",
                            clientSocket.getLocalAddress().getHostAddress(),
                            clientSocket.getLocalPort()
                    );
                } else {
                    LOGGER.warn(
                            "Failed to receive TCP request from {}:{}",
                            clientSocket.getLocalAddress().getHostAddress(),
                            clientSocket.getLocalPort()
                    );
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to read TCP request: {}", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private int handlePacket(InputStream input, OutputStream output, byte[] packet) throws IOException {
        byte[] buffer = new byte[2];
        int bytesRead = handleRead(input, buffer, 0, buffer.length);
        if (bytesRead != buffer.length) {
            return -1;
        }
        int packetLength = ((buffer[0] & 0xFF) << 8) + (buffer[1] & 0xFF);
        if (packetLength < 1 || packetLength > packet.length) {
            return -1;
        }
        bytesRead = handleRead(input, packet, 0, packetLength);
        if (bytesRead != packetLength) {
            return -1;
        }
        return bytesRead;
    }

    private int handleRead(InputStream input, byte[] buffer, int offset, int length) throws IOException {
        int totalBytesRead = 0;
        while (totalBytesRead < length) {
            int bytesRead = input.read(buffer, offset + totalBytesRead, length - totalBytesRead);
            if (bytesRead == -1) {
                break;
            }
            totalBytesRead += bytesRead;
        }
        return totalBytesRead;
    }

}