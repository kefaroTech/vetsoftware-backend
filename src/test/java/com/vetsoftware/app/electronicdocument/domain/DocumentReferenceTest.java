package com.vetsoftware.app.electronicdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * BillingReference UBL: a que factura apunta una nota credito/debito. Sin CUFE,
 * numero y fecha la DIAN no puede enlazar la correccion con el original, asi
 * que los tres son obligatorios. El prefijo si puede faltar (resoluciones sin
 * prefijo).
 */
@DisplayName("DocumentReference — referencia al documento corregido")
class DocumentReferenceTest {

    private static final LocalDate EMISION = LocalDate.of(2026, 3, 10);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    @DisplayName("rechaza una referencia sin CUFE")
    void rechaza_una_referencia_sin_cufe(String cufe) {
        assertThatThrownBy(() -> new DocumentReference(cufe, "SETP", 990L, EMISION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("referenced cufe is required");
    }

    @Test
    @DisplayName("rechaza una referencia sin numero de la factura original")
    void rechaza_una_referencia_sin_numero() {
        assertThatThrownBy(() -> new DocumentReference("CUFE-1", "SETP", null, EMISION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("referenced number is required");
    }

    @Test
    @DisplayName("rechaza una referencia sin fecha de emision de la factura original")
    void rechaza_una_referencia_sin_fecha() {
        assertThatThrownBy(() -> new DocumentReference("CUFE-1", "SETP", 990L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("referenced issueDate is required");
    }

    @Test
    @DisplayName("acepta una referencia sin prefijo: hay resoluciones que no lo llevan")
    void acepta_una_referencia_sin_prefijo() {
        assertThatCode(() -> new DocumentReference("CUFE-1", null, 990L, EMISION))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("conserva los cuatro datos del documento referenciado")
    void conserva_los_cuatro_datos() {
        DocumentReference reference = new DocumentReference("CUFE-1", "SETP", 990L, EMISION);

        assertThat(reference.cufe()).isEqualTo("CUFE-1");
        assertThat(reference.prefix()).isEqualTo("SETP");
        assertThat(reference.number()).isEqualTo(990L);
        assertThat(reference.issueDate()).isEqualTo(EMISION);
    }

    @Test
    @DisplayName("dos referencias al mismo documento son iguales: es una copia congelada")
    void dos_referencias_al_mismo_documento_son_iguales() {
        assertThat(new DocumentReference("CUFE-1", "SETP", 990L, EMISION))
                .isEqualTo(new DocumentReference("CUFE-1", "SETP", 990L, EMISION));
    }
}
