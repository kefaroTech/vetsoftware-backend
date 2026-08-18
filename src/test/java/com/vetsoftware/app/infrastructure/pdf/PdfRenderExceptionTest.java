package com.vetsoftware.app.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PdfRenderExceptionTest {

    @Test
    @DisplayName("conserva el mensaje cuando se construye solo con mensaje")
    void conserva_el_mensaje_cuando_se_construye_solo_con_mensaje() {
        PdfRenderException exception = new PdfRenderException("el HTML está vacío");

        assertThat(exception.getMessage()).isEqualTo("el HTML está vacío");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("conserva el mensaje y la causa cuando se construye con ambos")
    void conserva_el_mensaje_y_la_causa_cuando_se_construye_con_ambos() {
        RuntimeException cause = new RuntimeException("fallo del motor de render");

        PdfRenderException exception = new PdfRenderException("no fue posible generar el PDF",
                cause);

        assertThat(exception.getMessage()).isEqualTo("no fue posible generar el PDF");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
