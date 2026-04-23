package com.asterexcrisys.adblocker.tasks;

import com.asterexcrisys.adblocker.services.ProxyManager;
import com.asterexcrisys.adblocker.models.packets.UDPPacket;
import com.asterexcrisys.adblocker.services.contexts.ContextPool;
import com.asterexcrisys.adblocker.utilities.GlobalUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

@SuppressWarnings("unused")
public class UDPHandler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UDPHandler.class);

    private final ContextPool<ProxyManager> contextPool;
    private final BlockingQueue<UDPPacket> requests;
    private final BlockingQueue<UDPPacket> responses;

    public UDPHandler(ContextPool<ProxyManager> contextPool, BlockingQueue<UDPPacket> requests, BlockingQueue<UDPPacket> responses) {
        this.contextPool = Objects.requireNonNull(contextPool);
        this.requests = Objects.requireNonNull(requests);
        this.responses = Objects.requireNonNull(responses);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                UDPPacket requestPacket = requests.take();
                InetSocketAddress clientSocket = requestPacket.transport();
                Message request = new Message(requestPacket.data());
                Message response = GlobalUtility.acquireAccess(contextPool, (context) -> {
                    return context.handle(request);
                });
                UDPPacket responsePacket = UDPPacket.of(
                        requestPacket.transport(),
                        response.toWire()
                );
                if (responses.offer(responsePacket)) {
                    LOGGER.info(
                            "Succeeded to send UDP response to {}:{}",
                            clientSocket.getAddress().getHostAddress(),
                            clientSocket.getPort()
                    );
                } else {
                    LOGGER.warn(
                            "Failed to send UDP response to {}:{}",
                            clientSocket.getAddress().getHostAddress(),
                            clientSocket.getPort()
                    );
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to handle UDP request: {}", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}