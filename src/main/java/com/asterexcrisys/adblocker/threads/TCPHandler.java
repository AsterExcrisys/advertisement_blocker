package com.asterexcrisys.adblocker.threads;

import com.asterexcrisys.adblocker.services.ProxyManager;
import com.asterexcrisys.adblocker.types.TCPPacket;
import com.asterexcrisys.adblocker.types.ThreadContext;
import com.asterexcrisys.adblocker.utility.GlobalUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class TCPHandler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TCPHandler.class);

    private final ThreadLocal<ThreadContext> local;
    private final ThreadContext context;
    private final BlockingQueue<TCPPacket> requests;
    private final BlockingQueue<TCPPacket> responses;

    public TCPHandler(ThreadLocal<ThreadContext> local, BlockingQueue<TCPPacket> requests, BlockingQueue<TCPPacket> responses) {
        this.local = Objects.requireNonNull(local);
        this.context = Objects.requireNonNull(local.get());
        this.requests = Objects.requireNonNull(requests);
        this.responses = Objects.requireNonNull(responses);
    }

    @Override
    public void run() {
        try {
            ReentrantLock lock = context.lock();
            ProxyManager manager = context.manager();
            while (!Thread.currentThread().isInterrupted()) {
                TCPPacket requestPacket = requests.take();
                Socket clientSocket = requestPacket.socket();
                Message request = new Message(requestPacket.data());
                Message response = GlobalUtility.acquireAccess(lock, () -> manager.handle(request));
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
        } finally {
            context.remove();
            local.remove();
        }
    }

}