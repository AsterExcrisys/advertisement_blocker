package com.asterexcrisys.adblocker.threads;

import com.asterexcrisys.adblocker.services.ProxyManager;
import com.asterexcrisys.adblocker.types.ThreadContext;
import com.asterexcrisys.adblocker.types.UDPPacket;
import com.asterexcrisys.adblocker.utility.GlobalUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Message;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("unused")
public class UDPHandler implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(UDPHandler.class);

    private final ThreadLocal<ThreadContext> local;
    private final ThreadContext context;
    private final BlockingQueue<UDPPacket> requests;
    private final BlockingQueue<UDPPacket> responses;

    public UDPHandler(ThreadLocal<ThreadContext> local, BlockingQueue<UDPPacket> requests, BlockingQueue<UDPPacket> responses) {
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
                UDPPacket requestPacket = requests.take();
                Message request = new Message(requestPacket.data());
                Message response = GlobalUtility.acquireAccess(lock, () -> manager.handle(request));
                UDPPacket responsePacket = UDPPacket.of(
                        requestPacket.address(),
                        requestPacket.port(),
                        response.toWire()
                );
                if (responses.offer(responsePacket)) {
                    LOGGER.info("Succeeded to send UDP response to {}:{}", requestPacket.address(), requestPacket.port());
                } else {
                    LOGGER.warn("Failed to send UDP response to {}:{}", requestPacket.address(), requestPacket.port());
                }
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to handle UDP request: {}", exception.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            context.remove();
            local.remove();
        }
    }

}