package com.asterexcrisys.gab.resolvers;

import com.asterexcrisys.gab.utility.Utility;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import java.util.Objects;

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
            Message response = new Message(request.getHeader().getID());
            response.getHeader().setFlag(Flags.QR);
            response.addRecord(question, Section.QUESTION);
            Lookup lookup = new Lookup(
                    question.getName(),
                    question.getType(),
                    question.getDClass()
            );
            lookup.setResolver(new SimpleResolver(nameServer));
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
            return Utility.buildErrorResponse(
                    request,
                    Rcode.SERVFAIL,
                    2,
                    "Failed to resolve the DNS query: %s".formatted(e.getMessage())
            );
        }
    }

}