package com.asterexcrisys.adblocker.utility;

import org.xbill.DNS.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public final class GlobalUtility {

    public static <T> T synchronizeAccess(Object lock, Supplier<T> supplier) {
        synchronized (lock) {
            return supplier.get();
        }
    }

    public static <T> List<T> fillList(Supplier<T> supplier, int size) {
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(supplier.get());
        }
        return list;
    }

    public static Message buildErrorResponse(Message request, int statusCode, int optionCode, String optionMessage) {
        Message response = new Message(request.getHeader().getID());
        response.getHeader().setFlag(Flags.QR);
        response.getHeader().setRcode(statusCode);
        OPTRecord record = new OPTRecord(
                4096, 0, 0, 0,
                new GenericEDNSOption(15, buildAdditionalData(optionCode, optionMessage))
        );
        response.addRecord(request.getQuestion(), Section.QUESTION);
        response.addRecord(record, Section.ADDITIONAL);
        return response;
    }

    private static byte[] buildAdditionalData(int code, String reason) {
        byte[] data = new byte[2 + reason.getBytes().length];
        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.putShort((short) code);
        buffer.put(reason.getBytes(StandardCharsets.UTF_8));
        return data;
    }

}