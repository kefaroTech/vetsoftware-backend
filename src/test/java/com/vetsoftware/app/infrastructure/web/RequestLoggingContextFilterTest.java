package com.vetsoftware.app.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingContextFilterTest {

    private final RequestLoggingContextFilter filter = new RequestLoggingContextFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void ownsOnlyApplicationContextAndNeverTouchesTracingKeys() throws Exception {
        MDC.put("traceId", "21681c8c48fef44cf10671aec7948dfe");
        MDC.put("spanId", "10671aec7948dfe0");
        MDC.put(MdcKeys.ACTOR_TYPE, "STALE");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/owners");
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("User-Agent", "telemetry-test");
        AtomicReference<Map<String, String>> inside = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> inside.set(MDC.getCopyOfContextMap()));

        assertThat(inside.get()).containsEntry(MdcKeys.HTTP_METHOD, "POST")
                .containsEntry(MdcKeys.HTTP_PATH, "/api/v1/owners")
                .containsEntry(MdcKeys.CLIENT_IP, "192.0.2.10")
                .containsEntry(MdcKeys.USER_AGENT, "telemetry-test")
                .doesNotContainEntry(MdcKeys.ACTOR_TYPE, "STALE");

        assertThat(MDC.get("traceId")).isEqualTo("21681c8c48fef44cf10671aec7948dfe");
        assertThat(MDC.get("spanId")).isEqualTo("10671aec7948dfe0");
        assertThat(MDC.get(MdcKeys.HTTP_METHOD)).isNull();
        assertThat(MDC.get(MdcKeys.HTTP_PATH)).isNull();
        assertThat(MDC.get(MdcKeys.CLIENT_IP)).isNull();
        assertThat(MDC.get(MdcKeys.USER_AGENT)).isNull();
        assertThat(MDC.get(MdcKeys.ACTOR_TYPE)).isNull();
    }

    @Test
    void clearsTheSuperadminRequestIdEvenWhenTheServiceForgotToUnbindIt() throws Exception {
        // Hoy ningun servicio lo olvida: los seis de platformaccess desatan en su
        // propio finally. Este filtro existe justamente para no depender de que
        // todos ellos sigan haciendolo: el hilo es del pool de Tomcat y una clave
        // que sobreviva etiqueta la peticion del siguiente usuario con el id de una
        // solicitud de alta de superadministrador ajena.
        MDC.put(MdcKeys.SYSTEM_USER_REQUEST_ID, "4271");

        MockHttpServletRequest request = new MockHttpServletRequest("POST",
                "/platform/access-request");
        request.setRemoteAddr("192.0.2.10");
        AtomicReference<Map<String, String>> inside = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(),
                (ignoredRequest, ignoredResponse) -> {
                    inside.set(MDC.getCopyOfContextMap());
                    MDC.put(MdcKeys.SYSTEM_USER_REQUEST_ID, "9999");
                });

        // Entrando: la clave rancia ya se fue antes de la cadena.
        assertThat(inside.get()).doesNotContainKey(MdcKeys.SYSTEM_USER_REQUEST_ID);
        // Saliendo: lo que la cadena dejo puesto tampoco sobrevive.
        assertThat(MDC.get(MdcKeys.SYSTEM_USER_REQUEST_ID)).isNull();
    }
}
