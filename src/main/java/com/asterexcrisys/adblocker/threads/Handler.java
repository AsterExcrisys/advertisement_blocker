package com.asterexcrisys.adblocker.threads;

import com.asterexcrisys.adblocker.services.ProxyManager;
import com.asterexcrisys.adblocker.types.UDPPacket;
import com.asterexcrisys.adblocker.utility.GlobalUtility;
import org.xbill.DNS.Message;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

@SuppressWarnings("unused")
public class Handler extends Thread {

    private final ProxyManager manager;
    private final BlockingQueue<UDPPacket> requests;
    private final BlockingQueue<UDPPacket> responses;

    public Handler(ProxyManager manager, BlockingQueue<UDPPacket> requests, BlockingQueue<UDPPacket> responses) {
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
                Message response = GlobalUtility.synchronizeAccess(manager, () -> {
                    return manager.handle(request);
                });
                UDPPacket responsePacket = UDPPacket.of(
                        requestPacket.address(),
                        requestPacket.port(),
                        response.toWire()
                );
                if (responses.offer(responsePacket)) {
                    System.out.println("Succeeded to send response");
                } else {
                    System.out.println("Failed to send response");
                }
            }
        } catch (Exception e) {
            System.out.println("Exception caught while handling: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}