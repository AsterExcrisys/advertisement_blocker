package com.asterexcrisys.adblocker.tasks;

import com.asterexcrisys.adblocker.services.ProxyManager;
import com.asterexcrisys.adblocker.models.packets.TCPPacket;
import com.asterexcrisys.adblocker.services.contexts.ContextPool;
import com.asterexcrisys.adblocker.utilities.GlobalUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

@SuppressWarnings("unused")
public class TCPHandler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCPHandler.class);

    private final ContextPool<ProxyManager> contextPool;
    private final BlockingQueue<TCPPacket> requests;
    private final BlockingQueue<TCPPacket> responses;

    public TCPHandler(ContextPool<ProxyManager> contextPool, BlockingQueue<TCPPacket> requests, BlockingQueue<TCPPacket> responses) {
        this.contextPool = Objects.requireNonNull(contextPool);
        this.requests = Objects.requireNonNull(requests);
        this.responses = Objects.requireNonNull(responses);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                TCPPacket requestPacket = requests.take();
                Socket clientSocket = requestPacket.transport();
                Message request = new Message(requestPacket.data());
                Message response = GlobalUtility.acquireAccess(contextPool, (context) -> {
                    return context.handle(request);
                });
                TCPPacket responsePacket = TCPPacket.of(
                        clientSocket,
                        response.toWire()
                );
                if (responses.offer(responsePacket)) {
                    LOGGER.info(
                            "Succeeded to send TCP response to {}:{}",
                            clientSocket.getLocalAddress().getHostAddress(),
                            clientSocket.getLocalPort()
                    );
                } else {
                    LOGGER.warn(
                            "Failed to send TCP response to {}:{}",
                            clientSocket.getLocalAddress().getHostAddress(),
                            clientSocket.getLocalPort()
                    );
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to handle TCP request: {}", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}