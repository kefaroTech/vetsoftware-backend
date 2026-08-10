package com.vetsoftware.app.electronicdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Los mensajes de estas excepciones salen tal cual en el {@code ProblemDetail}
 * que ve el cajero, asi que el id del documento y el estado real forman parte
 * del contrato: sin ellos el soporte no puede ubicar el caso.
 */
@DisplayName("Excepciones de dominio de facturacion electronica")
class ElectronicDocumentExceptionsTest {

    @Test
    @DisplayName("el documento no encontrado nombra el id buscado")
    void el_documento_no_encontrado_nombra_el_id() {
        assertThat(new ElectronicDocumentNotFoundException(42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Electronic document not found: 42");
    }

    @ParameterizedTest
    @EnumSource(value = DianStatus.class, names = {"PENDIENTE", "RECHAZADO", "CONTINGENCIA",
            "NO_ELECTRONICO"})
    @DisplayName("el documento no validado reporta el estado real que impide corregirlo")
    void el_documento_no_validado_reporta_el_estado_real(DianStatus status) {
        assertThat(new DocumentNotValidatedException(42L, status))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("El documento 42")
                .hasMessageContaining("no esta VALIDADO")
                .hasMessageContaining("estado actual: " + status);
    }

    @Test
    @DisplayName("el documento ya reversado nombra el id de la factura anulada")
    void el_documento_ya_reversado_nombra_el_id() {
        assertThat(new DocumentAlreadyReversedException(42L)).isInstanceOf(RuntimeException.class)
                .hasMessage("El documento 42 ya fue reversado por una nota credito.");
    }

    @Test
    @DisplayName("el descuadre de kardex reporta documento, producto, esperado y descontado")
    void el_descuadre_de_kardex_reporta_los_cuatro_datos() {
        assertThat(new StockDiscountMismatchException(42L, 7L, 3, 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("El kardex no descontó lo vendido en el documento 42")
                .hasMessageContaining("producto 7").hasMessageContaining("se esperaban 3 unidades")
                .hasMessageContaining("se descontaron 1");
    }
}
