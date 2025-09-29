package com.asterexcrisys.adblocker;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TCPServerIntegrationTests {

    private Socket socket;

    @BeforeAll
    public void setUp() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress("127.0.0.1", 53));
    }

    @AfterAll
    public void tearDown() throws IOException {
        socket.close();
    }

}