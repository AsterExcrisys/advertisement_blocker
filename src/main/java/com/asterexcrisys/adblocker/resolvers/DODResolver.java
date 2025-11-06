package com.asterexcrisys.adblocker.resolvers;

import com.asterexcrisys.adblocker.services.SessionHandler;
import com.asterexcrisys.adblocker.utility.ResolverUtility;
import org.snf4j.core.DTLSSession;
import org.snf4j.core.SelectorLoop;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unused")
public final class DODResolver implements Resolver {

    private final String nameServer;
    private final int serverPort;
    private final SelectorLoop loop;

    public DODResolver(String nameServer) throws IOException {
        this.nameServer = Objects.requireNonNull(nameServer);
        this.serverPort = 853;
        loop = new SelectorLoop();
    }

    public DODResolver(String nameServer, int serverPort) throws IOException {
        this.nameServer = Objects.requireNonNull(nameServer);
        this.serverPort = serverPort;
        loop = new SelectorLoop();
    }

    public String nameServer() {
        return nameServer;
    }

    public int serverPort() {
        return serverPort;
    }

    @Override
    public Message resolve(Message request) {
        if (!ResolverUtility.validateRequest(request)) {
            return ResolverUtility.buildErrorResponse(
                    request,
                    Rcode.FORMERR,
                    400,
                    "Failed to resolve the DNS query: request must have a header and question field to be considered valid"
            );
        }
        try (DatagramChannel channel = DatagramChannel.open()) {
            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress(nameServer, serverPort));
            AtomicReference<byte[]> response = new AtomicReference<>(null);
            loop.start();
            loop.register(channel, new DTLSSession(new SessionHandler(request.toWire(), response), true));
            loop.join();
            if (response.get() == null) {
                return ResolverUtility.buildErrorResponse(
                        request,
                        Rcode.SERVFAIL,
                        500,
                        "Failed to resolve the DNS query: received no response from the upstream resolver"
                );
            }
            return new Message(response.get());
        } catch (Exception exception) {
            return ResolverUtility.buildErrorResponse(
                    request,
                    Rcode.SERVFAIL,
                    500,
                    "Failed to resolve the DNS query: %s".formatted(exception.getMessage())
            );
        } finally {
            loop.stop();
        }
    }

    @Override
    public void close() throws IOException {
        loop.stop();
    }

}