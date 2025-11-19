package com.asterexcrisys.adblocker;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TCPServerIntegrationTests {

    private Socket standardSocket;
    private Socket secureSocket;

    @BeforeAll
    public void setUp() throws IOException {
        standardSocket = new Socket();
        secureSocket = SSLSocketFactory.getDefault().createSocket();
        standardSocket.connect(new InetSocketAddress("127.0.0.1", 53));
        secureSocket.connect(new InetSocketAddress("127.0.0.1", 853));
    }

    @AfterAll
    public void tearDown() throws IOException {
        standardSocket.close();
        secureSocket.close();
    }

}