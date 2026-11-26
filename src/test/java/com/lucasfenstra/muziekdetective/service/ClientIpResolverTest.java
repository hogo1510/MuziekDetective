package com.lucasfenstra.muziekdetective.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private static MockHttpServletRequest request(String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    @Test
    void achterProxyWordtHetLaatsteItemGebruikt() {
        ClientIpResolver resolver = new ClientIpResolver(true);
        // "1.1.1.1" is door de client zelf meegestuurd, "8.8.8.8" heeft de proxy toegevoegd
        assertThat(resolver.resolve(request("1.1.1.1, 8.8.8.8"))).isEqualTo("8.8.8.8");
    }

    @Test
    void achterProxyZonderHeaderValtTerugOpRemoteAddr() {
        assertThat(new ClientIpResolver(true).resolve(request(null))).isEqualTo("10.0.0.1");
    }

    @Test
    void zonderProxyWordtDeHeaderGenegeerd() {
        assertThat(new ClientIpResolver(false).resolve(request("1.1.1.1"))).isEqualTo("10.0.0.1");
    }
}
