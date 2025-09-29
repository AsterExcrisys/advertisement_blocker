package com.asterexcrisys.adblocker;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UDPServerIntegrationTests {

    private DatagramSocket socket;

    @BeforeAll
    public void setUp() throws SocketException {
        socket = new DatagramSocket();
        socket.connect(new InetSocketAddress("127.0.0.1", 53));
    }

    @AfterAll
    public void tearDown() {
        socket.close();
    }

}