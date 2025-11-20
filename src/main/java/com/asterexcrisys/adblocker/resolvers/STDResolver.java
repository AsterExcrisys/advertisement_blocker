package com.asterexcrisys.adblocker.resolvers;

import com.asterexcrisys.adblocker.models.types.DNSProtocol;
import com.asterexcrisys.adblocker.utilities.ResolverUtility;
import org.xbill.DNS.*;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;

@SuppressWarnings("unused")
public record STDResolver(String nameServer, DNSProtocol dnsProtocol) implements Resolver {

    public STDResolver(String nameServer) {
        this(nameServer, DNSProtocol.UDP);
    }

    public STDResolver(String nameServer, DNSProtocol dnsProtocol) {
        this.nameServer = Objects.requireNonNull(nameServer);
        this.dnsProtocol = Objects.requireNonNull(dnsProtocol);
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
            SimpleResolver resolver = new SimpleResolver(nameServer);
            resolver.setTCP(dnsProtocol != DNSProtocol.UDP);
            resolver.setIgnoreTruncation(true);
            resolver.setTimeout(Duration.ofMillis(5000));
            OPTRecord record = request.getOPT();
            if (record != null) {
                resolver.setEDNS(
                        record.getVersion(),
                        4096,
                        record.getFlags(),
                        record.getOptions()
                );
            } else {
                resolver.setEDNS(0, 4096, 0, Collections.emptyList());
            }
            return resolver.send(request);
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