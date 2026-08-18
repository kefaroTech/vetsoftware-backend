package com.vetsoftware.app.companytaxprofile.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.DisplayName;

/**
 * Codigo DIAN de cada tipo de documento del emisor/adquiriente. Nadie en
 * produccion llama todavia a {@code dianCode()} — queda listo para cuando la
 * factura electronica necesite el codigo del catalogo 6343 en vez del enum Java
 * — asi que este test es lo unico que fija el valor frente a un typo.
 */
@DisplayName("CompanyDocumentType.dianCode")
class CompanyDocumentTypeTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"NIT, 31", "CEDULA_CIUDADANIA, 13", "CEDULA_EXTRANJERIA, 22", "PASAPORTE, 41"})
    @DisplayName("expone el codigo del catalogo DIAN de cada tipo de documento")
    void expone_el_codigo_dian_de_cada_tipo(CompanyDocumentType tipo, int dianCode) {
        assertThat(tipo.dianCode()).isEqualTo(dianCode);
    }
}
