package com.asterexcrisys.adblocker.services.sockets;

import org.bouncycastle.tls.*;

@SuppressWarnings("unused")
public class SocketAuthentication implements TlsAuthentication {


    @Override
    public void notifyServerCertificate(TlsServerCertificate certificate) {

    }

    @Override
    public TlsCredentials getClientCredentials(CertificateRequest request) {
        return null;
    }

}