package com.asterexcrisys.adblocker.threads;

import com.asterexcrisys.adblocker.services.ProxyManager;
import com.asterexcrisys.adblocker.types.UDPPacket;
import com.asterexcrisys.adblocker.utility.GlobalUtility;
import org.xbill.DNS.Message;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("unused")
public class Handler extends Thread {

    private final ReentrantLock lock;
    private final ProxyManager manager;
    private final BlockingQueue<UDPPacket> requests;
    private final BlockingQueue<UDPPacket> responses;

    public Handler(ReentrantLock lock, ProxyManager manager, BlockingQueue<UDPPacket> requests, BlockingQueue<UDPPacket> responses) {
        this.lock = Objects.requireNonNull(lock);
        this.manager = Objects.requireNonNull(manager);
        this.requests = Objects.requireNonNull(requests);
        this.responses = Objects.requireNonNull(responses);
    }

    @Override
    public void run() {
        try {
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
                    System.out.printf("Information: succeeded to send response to %s:%s\n", requestPacket.address(), requestPacket.port());
                } else {
                    System.out.printf("Warning: failed to send response to %s:%s\n", requestPacket.address(), requestPacket.port());
                }
            }
        } catch (Exception exception) {
            System.err.printf("Error: %s\n", exception.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}