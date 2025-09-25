package com.asterexcrisys.adblocker.utility;

import org.xbill.DNS.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
public final class DNSUtility {

    public static void updatePayloadSize(Message request) {
        OPTRecord oldRecord = request.getOPT();
        OPTRecord newRecord = new OPTRecord(
                4096,
                oldRecord.getExtendedRcode(),
                oldRecord.getVersion(),
                oldRecord.getFlags(),
                oldRecord.getOptions()
        );
        request.removeAllRecords(Section.ADDITIONAL);
        request.addRecord(newRecord, Section.ADDITIONAL);
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