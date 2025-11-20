package com.asterexcrisys.adblocker.resolvers;

import com.asterexcrisys.adblocker.utilities.ResolverUtility;
import org.xbill.DNS.Message;
import org.xbill.DNS.Rcode;
import javax.net.ssl.SSLSocketFactory;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Objects;

@SuppressWarnings("unused")
public final class DOTResolver implements Resolver {

    private final String nameServer;
    private final int serverPort;
    private final SSLSocketFactory socketFactory;

    public DOTResolver(String nameServer) {
        this.nameServer = Objects.requireNonNull(nameServer);
        serverPort = 853;
        socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    public DOTResolver(String nameServer, int serverPort) {
        this.nameServer = Objects.requireNonNull(nameServer);
        this.serverPort = serverPort;
        socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
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
        try (Socket socket = socketFactory.createSocket(nameServer, serverPort)) {
            socket.setSoTimeout(5000);
            OutputStream output = socket.getOutputStream();
            InputStream input = socket.getInputStream();
            ResolverUtility.updatePayloadSize(request);
            byte[] data = request.toWire();
            output.write((data.length >> 8) & 0xFF);
            output.write(data.length & 0xFF);
            output.write(data);
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
        }
    }

    @Override
    public void close() {}

}