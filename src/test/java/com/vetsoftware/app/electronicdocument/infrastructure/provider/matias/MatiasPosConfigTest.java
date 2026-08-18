package com.vetsoftware.app.electronicdocument.infrastructure.provider.matias;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatiasPosConfig — datos operativos del POS y del fabricante del software")
class MatiasPosConfigTest {

    @Test
    @DisplayName("expone cada valor recibido tal cual, campo por campo")
    void expone_cada_valor_recibido_tal_cual() {
        MatiasPosConfig config = new MatiasPosConfig("CJ002", "Caja secundaria", "POS02",
                "Cra 1 # 2-3", "Cajero Ana", "Duenio SAS", "VetSoftware SAS",
                "VetSoftware Facturacion");

        assertThat(config.terminalNumber()).isEqualTo("CJ002");
        assertThat(config.cashierType()).isEqualTo("Caja secundaria");
        assertThat(config.salesCode()).isEqualTo("POS02");
        assertThat(config.address()).isEqualTo("Cra 1 # 2-3");
        assertThat(config.defaultCashier()).isEqualTo("Cajero Ana");
        assertThat(config.softwareOwnerName()).isEqualTo("Duenio SAS");
        assertThat(config.softwareCompanyName()).isEqualTo("VetSoftware SAS");
        assertThat(config.softwareName()).isEqualTo("VetSoftware Facturacion");
    }
}
