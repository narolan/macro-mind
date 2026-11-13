package com.macromind.infra;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

/**
 * A TLS context that trusts any certificate. The League client serves a self-signed certificate, so there is nothing
 * to validate against. Only ever use this for a loopback connection: it provides no protection against an impostor.
 */
final class LoopbackTlsContext {

    private LoopbackTlsContext() {}

    static SSLContext create() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{new TrustEverything()}, null);
            return context;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not create the TLS context for the local game client", e);
        }
    }

    // Extended trust manager also skips hostname verification (the self-signed cert has no 127.0.0.1 SAN).
    private static final class TrustEverything extends X509ExtendedTrustManager {
        @Override public void checkClientTrusted(X509Certificate[] chain, String authType) { }
        @Override public void checkServerTrusted(X509Certificate[] chain, String authType) { }
        @Override public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) { }
        @Override public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) { }
        @Override public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) { }
        @Override public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) { }
        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }
}
