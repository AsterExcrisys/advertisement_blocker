package com.asterexcrisys.gab.utility;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;
import java.util.Optional;

@SuppressWarnings("unused")
public class DNSCache {

    private final Cache cache;

    public DNSCache() {
        cache = new Cache(DClass.IN);
    }

    public DNSCache(int maximumSize) {
        if (maximumSize < 1) {
            throw new IllegalArgumentException();
        }
        cache = new Cache(DClass.IN);
        cache.setMaxEntries(maximumSize);
    }

    public int getMaximumSize() {
        return cache.getMaxEntries();
    }

    public void setMaximumSize(int maximumSize) {
        cache.setMaxEntries(maximumSize);
    }

    public void put(Message response) {
        if (response == null) {
            throw new IllegalArgumentException();
        }
        for (Record answer : response.getSection(Section.ANSWER)) {
            cache.addRecord(answer, Credibility.NORMAL);
        }
    }

    public void remove(Message request, boolean byType) {
        if (request == null || request.getQuestion() == null) {
            return;
        }
        if (byType) {
            cache.flushSet(request.getQuestion().getName(), request.getQuestion().getType());
        } else {
            cache.flushName(request.getQuestion().getName());
        }
    }

    public void clear() {
        cache.clearCache();
    }

    public Optional<Message> get(Message request) {
        if (request == null || request.getQuestion() == null) {
            return Optional.empty();
        }
        SetResponse result = cache.lookupRecords(
                request.getQuestion().getName(),
                request.getQuestion().getType(),
                Credibility.NORMAL
        );
        if (!result.isSuccessful()) {
            return Optional.empty();
        }
        Message response = new Message(request.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setRcode(Rcode.NOERROR);
        response.addRecord(request.getQuestion(), Section.QUESTION);
        for (RRset answers : result.answers()) {
            for (Record answer : answers) {
                response.addRecord(answer, Section.ANSWER);
            }
        }
        return Optional.of(response);
    }

}