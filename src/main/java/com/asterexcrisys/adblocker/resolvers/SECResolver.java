package com.asterexcrisys.adblocker.resolvers;

import com.asterexcrisys.adblocker.models.types.DNSProtocol;
import com.asterexcrisys.adblocker.utility.ResolverUtility;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import org.xbill.DNS.dnssec.ValidatingResolver;
import java.io.FileInputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("unused")
public final class SECResolver implements Resolver {

    private final String trustAnchor;
    private final String nameServer;
    private final DNSProtocol dnsProtocol;

    public SECResolver(String nameServer) {
        this.nameServer = Objects.requireNonNull(nameServer);
        trustAnchor = null;
        dnsProtocol = DNSProtocol.UDP;
    }

    public SECResolver(String nameServer, String trustAnchor) {
        this.nameServer = Objects.requireNonNull(nameServer);
        this.trustAnchor = Objects.requireNonNull(trustAnchor);
        dnsProtocol = DNSProtocol.UDP;
    }

    public SECResolver(String nameServer, DNSProtocol dnsProtocol) {
        this.nameServer = Objects.requireNonNull(nameServer);
        trustAnchor = null;
        this.dnsProtocol = Objects.requireNonNull(dnsProtocol);
    }

    public SECResolver(String nameServer, String trustAnchor, DNSProtocol dnsProtocol) {
        this.nameServer = Objects.requireNonNull(nameServer);
        this.trustAnchor = Objects.requireNonNull(trustAnchor);
        this.dnsProtocol = Objects.requireNonNull(dnsProtocol);
    }

    public String trustAnchor() {
        return trustAnchor;
    }

    public String nameServer() {
        return nameServer;
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
            resolver.setTCP(dnsProtocol != DNSProtocol.UDP);
            resolver.setIgnoreTruncation(true);
            resolver.setTimeout(Duration.ofMillis(5000));
            if (trustAnchor != null) {
                resolver.loadTrustAnchors(new FileInputStream(trustAnchor));
            }
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