package com.vetsoftware.app.tax.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("TaxScheme")
class TaxSchemeTest {

    @ParameterizedTest(name = "{0} usa el codigo DIAN {1}")
    @CsvSource({"IVA, 01", "INC, 04"})
    @DisplayName("cada esquema expone su codigo DIAN")
    void cada_esquema_expone_su_codigo_dian(TaxScheme scheme, String dianCode) {
        assertThat(scheme.dianCode()).isEqualTo(dianCode);
    }
}
