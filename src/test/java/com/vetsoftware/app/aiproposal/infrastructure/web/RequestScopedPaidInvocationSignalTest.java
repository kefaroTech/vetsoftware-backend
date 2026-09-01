package com.vetsoftware.app.aiproposal.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.vetsoftware.app.shared.ai.PaidInvocationMark;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * El adaptador que escribe el bit en la petición HTTP en curso, que es donde
 * {@code LoginRateLimitFilter} lo lee al volver de la cadena.
 */
@DisplayName("RequestScopedPaidInvocationSignal — el bit se escribe en la petición, no en el bean")
class RequestScopedPaidInvocationSignalTest {

    private final RequestScopedPaidInvocationSignal signal = new RequestScopedPaidInvocationSignal();

    @AfterEach
    void desligar() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static MockHttpServletRequest peticionLigada() {
        MockHttpServletRequest peticion = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(peticion));
        return peticion;
    }

    @Test
    @DisplayName("«no hubo invocación» queda escrito en la petición ligada al hilo")
    void sin_invocacion_queda_escrito() {
        MockHttpServletRequest peticion = peticionLigada();

        signal.signal(false);

        assertThat(PaidInvocationMark.constaQueNoHuboInvocacion(peticion)).isTrue();
    }

    @Test
    @DisplayName("«sí hubo invocación» también se escribe: la ausencia de marca tiene que"
            + " significar una sola cosa")
    void con_invocacion_tambien_se_escribe() {
        MockHttpServletRequest peticion = peticionLigada();

        signal.signal(true);

        assertThat(peticion.getAttribute(PaidInvocationMark.ATRIBUTO)).isEqualTo(Boolean.TRUE);
        assertThat(PaidInvocationMark.constaQueNoHuboInvocacion(peticion)).isFalse();
    }

    /**
     * ⛔ Este es el caso legítimo, no el defensivo: un test unitario, un hilo de
     * tareas o cualquier invocación fuera de un servlet. No hay cupo que devolver
     * porque no hubo filtro que lo consumiera, y lo que no puede pasar es que
     * reviente.
     */
    @Test
    @DisplayName("sin petición ligada al hilo no hace nada y no lanza")
    void sin_peticion_ligada_no_lanza() {
        RequestContextHolder.resetRequestAttributes();

        assertThatCode(() -> signal.signal(false)).doesNotThrowAnyException();
    }
}
