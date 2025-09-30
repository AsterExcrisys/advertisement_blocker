package com.asterexcrisys.adblocker.resolvers;

import org.junit.jupiter.api.*;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import java.io.IOException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DOQResolverUnitTests {

    private Resolver resolver;

    @BeforeAll
    public void setUp() {
        resolver = new DOQResolver("1.1.1.2");
    }

    @AfterAll
    public void tearDown() throws Exception {
        resolver.close();
    }

    @Test
    public void shouldReturnNoErrorWhenValidExistentRequest() throws TextParseException {
        Name name = Name.fromString("cloudflare.com.", Name.root);
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message request = Message.newQuery(question);
        Message response = resolver.resolve(request);
        Assertions.assertEquals(Rcode.NOERROR, response.getHeader().getRcode());
    }

    @Test
    public void shouldReturnNxDomainWhenValidNonExistentRequest() throws TextParseException {
        Name name = Name.fromString("subdomain.example.com.", Name.root);
        Record question = Record.newRecord(name, Type.A, DClass.IN);
        Message request = Message.newQuery(question);
        Message response = resolver.resolve(request);
        Assertions.assertEquals(Rcode.NXDOMAIN, response.getHeader().getRcode());
    }

    @Test
    public void shouldReturnFormErrWhenInvalidRequest() throws IOException {
        Header header = new Header(1);
        header.setOpcode(Opcode.QUERY);
        header.setFlag(Flags.RD);
        Message request = new Message(header.toWire());
        Message response = resolver.resolve(request);
        Assertions.assertEquals(Rcode.FORMERR, response.getHeader().getRcode());
    }

}