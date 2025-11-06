package com.asterexcrisys.adblocker.services;

import org.snf4j.core.handler.AbstractDatagramHandler;
import org.snf4j.core.handler.SessionEvent;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public class SessionHandler extends AbstractDatagramHandler {

    private final byte[] request;
    private final AtomicReference<byte[]> response;
    private boolean isRequestSent;
    private boolean isResponseReceived;

    public SessionHandler(byte[] request, AtomicReference<byte[]> response) {
        this.request = Objects.requireNonNull(request);
        this.response = Objects.requireNonNull(response);
        isRequestSent = false;
        isResponseReceived = false;
    }

    @Override
    public void event(SessionEvent event) {
        if (isRequestSent || event != SessionEvent.READY) {
            return;
        }
        getSession().send(getSession().getRemoteAddress(), request);
        isRequestSent = true;
    }

    @Override
    public void read(SocketAddress address, byte[] data) {
        if (isResponseReceived || data == null) {
            return;
        }
        getSession().close();
        response.set(data);
        isResponseReceived = true;
    }

    @Override
    public void read(SocketAddress address, Object data) {}

    @Override
    public void read(Object data) {}

}