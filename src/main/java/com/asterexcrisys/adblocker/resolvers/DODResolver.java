package com.asterexcrisys.adblocker.resolvers;

import com.asterexcrisys.adblocker.services.sockets.SocketTransport;
import com.asterexcrisys.adblocker.utility.GlobalUtility;
import com.asterexcrisys.adblocker.utility.ResolverUtility;
import org.bouncycastle.tls.*;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Objects;

@SuppressWarnings("unused")
public final class DODResolver implements Resolver {

    private final String nameServer;
    private final int serverPort;
    private final DTLSClientProtocol protocol;
    private final DatagramTransport transport;

    public DODResolver(String nameServer) throws SocketException, UnknownHostException {
        this.nameServer = Objects.requireNonNull(nameServer);
        this.serverPort = 853;
        protocol = new DTLSClientProtocol();
        transport = new SocketTransport(InetAddress.getByName(this.nameServer), this.serverPort, 1500);
    }

    public DODResolver(String nameServer, int serverPort) throws SocketException, UnknownHostException {
        this.nameServer = Objects.requireNonNull(nameServer);
        this.serverPort = serverPort;
        protocol = new DTLSClientProtocol();
        transport = new SocketTransport(InetAddress.getByName(this.nameServer), this.serverPort, 1500);
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
        try {
            return GlobalUtility.tryWith(protocol.connect(null, transport), (transport) -> {
                return request;
            });
        } catch (Exception exception) {
            return ResolverUtility.buildErrorResponse(
                    request,
                    Rcode.SERVFAIL,
                    500,
                    "Failed to resolve the DNS query: %s".formatted(exception.getMessage())
            );
        }
    }

    @Override
    public void close() throws IOException {
        transport.close();
    }

}