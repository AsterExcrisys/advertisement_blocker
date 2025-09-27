package com.asterexcrisys.adblocker.resolvers;

import com.asterexcrisys.adblocker.utility.DNSUtility;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;

@SuppressWarnings("unused")
public record STDResolver(String nameServer) implements Resolver {

    public STDResolver {
        Objects.requireNonNull(nameServer);
    }

    @Override
    public Message resolve(Message request) {
        try {
            Record question = request.getQuestion();
            if (question == null) {
                throw new IllegalArgumentException("No question found");
            }
            SimpleResolver resolver = new SimpleResolver(nameServer);
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
            return DNSUtility.buildErrorResponse(
                    request,
                    Rcode.SERVFAIL,
                    2,
                    "Failed to resolve the DNS query: %s".formatted(exception.getMessage())
            );
        }
    }

}