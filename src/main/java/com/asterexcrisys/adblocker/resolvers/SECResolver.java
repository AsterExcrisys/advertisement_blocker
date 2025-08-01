package com.asterexcrisys.adblocker.resolvers;

import com.asterexcrisys.adblocker.utility.GlobalUtility;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import org.xbill.DNS.dnssec.ValidatingResolver;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@SuppressWarnings("unused")
public record SECResolver(String trustAnchor, String nameServer) implements Resolver {

    public SECResolver {
        Objects.requireNonNull(trustAnchor);
        Objects.requireNonNull(nameServer);
    }

    @Override
    public Message resolve(Message request) {
        try {
            Record question = request.getQuestion();
            if (question == null) {
                throw new IllegalArgumentException("No question found");
            }
            Message response = new Message(request.getHeader().getID());
            response.getHeader().setFlag(Flags.QR);
            response.addRecord(question, Section.QUESTION);
            Lookup lookup = new Lookup(
                    question.getName(),
                    question.getType(),
                    question.getDClass()
            );
            ValidatingResolver resolver = new ValidatingResolver(new SimpleResolver(nameServer));
            resolver.loadTrustAnchors(new ByteArrayInputStream(trustAnchor.getBytes(StandardCharsets.UTF_8)));
            lookup.setResolver(resolver);
            Record[] records = lookup.run();
            if (lookup.getResult() == Lookup.SUCCESSFUL && records != null) {
                for (Record record : records) {
                    response.addRecord(record, Section.ANSWER);
                }
            } else {
                response.getHeader().setRcode(lookup.getResult());
            }
            return response;
        } catch (Exception e) {
            return GlobalUtility.buildErrorResponse(
                    request,
                    Rcode.SERVFAIL,
                    2,
                    "Failed to resolve the DNS query: %s".formatted(e.getMessage())
            );
        }
    }

}