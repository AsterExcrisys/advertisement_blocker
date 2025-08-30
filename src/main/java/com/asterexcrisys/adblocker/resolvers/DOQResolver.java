package com.asterexcrisys.adblocker.resolvers;

import com.asterexcrisys.adblocker.utility.DNSUtility;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import tech.kwik.core.QuicClientConnection;
import tech.kwik.core.QuicStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Objects;

@SuppressWarnings("unused")
public record DOQResolver(String nameServer) implements Resolver {

    private static final String APPLICATION_PROTOCOL = "doq";

    public DOQResolver {
        Objects.requireNonNull(nameServer);
    }

    @Override
    public Message resolve(Message request) {
        QuicClientConnection connection = null;
        try {
            connection = QuicClientConnection.newBuilder()
                    .uri(URI.create(nameServer))
                    .applicationProtocol(APPLICATION_PROTOCOL)
                    .build();
            connection.connect();
            QuicStream stream = connection.createStream(true);
            OutputStream output = stream.getOutputStream();
            InputStream input = stream.getInputStream();
            byte[] bytes = request.toWire();
            output.write((bytes.length >> 8) & 0xFF);
            output.write(bytes.length & 0xFF);
            output.write(bytes);
            output.flush();
            int length = (input.read() << 8) | input.read();
            return new Message(input.readNBytes(length));
        } catch (Exception exception) {
            return DNSUtility.buildErrorResponse(
                    request,
                    Rcode.SERVFAIL,
                    2,
                    "Failed to resolve the DNS query: %s".formatted(exception.getMessage())
            );
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

}