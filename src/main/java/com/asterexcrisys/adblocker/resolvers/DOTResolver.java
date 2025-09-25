package com.asterexcrisys.adblocker.resolvers;

import com.asterexcrisys.adblocker.utility.DNSUtility;
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
        try (Socket socket = socketFactory.createSocket(nameServer, serverPort)) {
            socket.setSoTimeout(5000);
            OutputStream output = socket.getOutputStream();
            InputStream input = socket.getInputStream();
            DNSUtility.updatePayloadSize(request);
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
        }
    }

}