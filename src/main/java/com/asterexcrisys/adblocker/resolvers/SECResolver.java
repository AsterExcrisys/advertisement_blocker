package com.asterexcrisys.adblocker.resolvers;

import com.asterexcrisys.adblocker.utility.ResolverUtility;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import org.xbill.DNS.dnssec.ValidatingResolver;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public record SECResolver(String trustAnchor, String nameServer) implements Resolver {

    public SECResolver {
        Objects.requireNonNull(trustAnchor);
        Objects.requireNonNull(nameServer);
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
            ValidatingResolver resolver = new ValidatingResolver(new SimpleResolver(nameServer));
            resolver.setTimeout(Duration.ofMillis(5000));
            resolver.loadTrustAnchors(new ByteArrayInputStream(trustAnchor.getBytes(StandardCharsets.UTF_8)));
            OPTRecord record = request.getOPT();
            boolean isSecure;
            if (record != null) {
                resolver.setEDNS(
                        record.getVersion(),
                        4096,
                        record.getFlags() | ExtendedFlags.DO,
                        record.getOptions()
                );
                isSecure = (record.getFlags() & ExtendedFlags.DO) == ExtendedFlags.DO;
            } else {
                resolver.setEDNS(0, 4096, ExtendedFlags.DO, Collections.emptyList());
                isSecure = false;
            }
            Message response = resolver.send(request);
            if (!isSecure) {
                stripAdditionalRecords(response);
            }
            return response;
        } catch (Exception exception) {
            return ResolverUtility.buildErrorResponse(
                    request,
                    Rcode.SERVFAIL,
                    500,
                    "Failed to resolve the DNS query: %s".formatted(exception.getMessage())
            );
        }
    }

    private void stripAdditionalRecords(Message response) {
        for (int section : new int[] {Section.ANSWER, Section.AUTHORITY, Section.ADDITIONAL}) {
            List<Record> records = new ArrayList<>();
            for (Record record : response.getSection(section)) {
                int type = record.getType();
                if (type == Type.RRSIG || type == Type.DNSKEY || type == Type.NSEC || type == Type.NSEC3) {
                    records.add(record);
                }
            }
            for (Record record : records) {
                response.removeRecord(record, section);
            }
        }
        OPTRecord oldRecord = response.getOPT();
        if (oldRecord != null) {
            OPTRecord newRecord = new OPTRecord(
                    oldRecord.getPayloadSize(),
                    oldRecord.getExtendedRcode(),
                    oldRecord.getVersion(),
                    oldRecord.getFlags() & ~ExtendedFlags.DO,
                    oldRecord.getOptions()
            );
            response.removeAllRecords(Section.ADDITIONAL);
            response.addRecord(newRecord, Section.ADDITIONAL);
        }
    }

    @Override
    public void close() {}

}