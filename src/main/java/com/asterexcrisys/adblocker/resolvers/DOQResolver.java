package com.asterexcrisys.adblocker.resolvers;

import com.asterexcrisys.adblocker.utility.ResolverUtility;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import tech.kwik.core.QuicClientConnection;
import tech.kwik.core.QuicConnection;
import tech.kwik.core.QuicStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@SuppressWarnings("unused")
public record DOQResolver(String nameServer, int serverPort) implements Resolver {

    private static final String APPLICATION_PROTOCOL = "doq";

    public DOQResolver(String nameServer) {
        this(nameServer, 853);
    }

    public DOQResolver(String nameServer, int serverPort) {
        this.nameServer = Objects.requireNonNull(nameServer);
        this.serverPort = serverPort;
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
        QuicClientConnection connection = null;
        try {
            connection = QuicClientConnection.newBuilder()
                    .uri(URI.create("quic://%s:%s".formatted(nameServer, serverPort)))
                    .applicationProtocol(APPLICATION_PROTOCOL)
                    .connectTimeout(Duration.ofMillis(3000))
                    .maxIdleTimeout(Duration.ofMillis(5000))
                    .version(QuicConnection.QuicVersion.V2)
                    .build();
            connection.connect();
            QuicStream stream = connection.createStream(true);
            OutputStream output = stream.getOutputStream();
            InputStream input = stream.getInputStream();
            ResolverUtility.updatePayloadSize(request);
            output.write(request.toWire());
            output.flush();
            int length = (input.read() << 8) | input.read();
            return new Message(input.readNBytes(length));
        } catch (Exception exception) {
            return ResolverUtility.buildErrorResponse(
                    request,
                    Rcode.SERVFAIL,
                    500,
                    "Failed to resolve the DNS query: %s".formatted(exception.getMessage())
            );
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    @Override
    public void close() {}

}