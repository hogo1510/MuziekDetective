package com.lucasfenstra.muziekdetective.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Bepaalt het IP-adres van de bezoeker voor de rate limiter.
 *
 * Achter een reverse proxy (Railway) staat het echte IP in {@code X-Forwarded-For}.
 * Een client kan die header zelf meesturen, maar de proxy voegt het IP dat hij ziet
 * achteraan toe. Daarom nemen we het laatste item, niet het eerste. Zonder proxy
 * ({@code trustProxy=false}) negeren we de header helemaal, want dan is hij volledig
 * door de client te kiezen.
 */
@Component
public class ClientIpResolver {

    private final boolean trustProxy;

    public ClientIpResolver(@Value("${muziekdetective.trust-proxy:false}") boolean trustProxy) {
        this.trustProxy = trustProxy;
    }

    public String resolve(HttpServletRequest request) {
        if (trustProxy) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String[] parts = forwardedFor.split(",");
                String last = parts[parts.length - 1].trim();
                if (!last.isEmpty()) {
                    return last;
                }
            }
        }
        return request.getRemoteAddr();
    }
}
