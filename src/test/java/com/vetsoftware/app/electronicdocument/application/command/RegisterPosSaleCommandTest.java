package com.vetsoftware.app.electronicdocument.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand.SaleLine;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RegisterPosSaleCommand.hasGeneralLine() — gate «solo SYSTEM» del
 * {@code @PreAuthorize} del puerto. No lo toca ningun test de servicio ni de
 * controller (vive en el SpEL, no en el flujo normal), asi que necesita su
 * propio test invocandolo directamente.
 */
@DisplayName("RegisterPosSaleCommand.hasGeneralLine — gate de lineas de precio libre")
class RegisterPosSaleCommandTest {

    private static RegisterPosSaleCommand comandoConLineas(List<SaleLine> lines) {
        return new RegisterPosSaleCommand(9L, ElectronicDocumentType.DOC_EQUIV_POS, true, null,
                lines, List.of(), "req-1", 4L, 7L);
    }

    private static SaleLine linea(SaleLineKind kind) {
        return new SaleLine(kind, 1L, "linea", BigDecimal.ONE, new BigDecimal("1000"));
    }

    @Test
    @DisplayName("una lista de lineas null no tiene linea GENERAL")
    void lista_null_no_tiene_general() {
        assertThat(comandoConLineas(null).hasGeneralLine()).isFalse();
    }

    @Test
    @DisplayName("una lista vacia no tiene linea GENERAL")
    void lista_vacia_no_tiene_general() {
        assertThat(comandoConLineas(List.of()).hasGeneralLine()).isFalse();
    }

    @Test
    @DisplayName("una lista con solo lineas GENERAL la detecta")
    void solo_general_la_detecta() {
        assertThat(comandoConLineas(List.of(linea(SaleLineKind.GENERAL))).hasGeneralLine())
                .isTrue();
    }

    @Test
    @DisplayName("una lista con solo lineas no-GENERAL no la detecta")
    void solo_no_general_no_la_detecta() {
        assertThat(
                comandoConLineas(List.of(linea(SaleLineKind.PRODUCT), linea(SaleLineKind.SERVICE)))
                        .hasGeneralLine())
                .isFalse();
    }

    @Test
    @DisplayName("una mezcla con al menos una GENERAL la detecta")
    void mezcla_con_una_general_la_detecta() {
        assertThat(comandoConLineas(List.of(linea(SaleLineKind.PRODUCT),
                linea(SaleLineKind.GENERAL), linea(SaleLineKind.SERVICE))).hasGeneralLine())
                .isTrue();
    }

    @Test
    @DisplayName("varias lineas GENERAL entre otras tambien la detecta")
    void varias_general_tambien_la_detecta() {
        assertThat(
                comandoConLineas(List.of(linea(SaleLineKind.GENERAL), linea(SaleLineKind.GENERAL)))
                        .hasGeneralLine())
                .isTrue();
    }
}
