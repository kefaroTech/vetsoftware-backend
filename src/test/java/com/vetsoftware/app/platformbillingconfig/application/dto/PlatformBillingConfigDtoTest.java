package com.vetsoftware.app.platformbillingconfig.application.dto;

import static com.vetsoftware.app.platformbillingconfig.testsupport.PlatformBillingConfigMother.CREADA;
import static com.vetsoftware.app.platformbillingconfig.testsupport.PlatformBillingConfigMother.configurada;
import static com.vetsoftware.app.platformbillingconfig.testsupport.PlatformBillingConfigMother.sinTarifa;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlatformBillingConfigDto — proyección de salida")
class PlatformBillingConfigDtoTest {

    @Test
    @DisplayName("copia cada política y la tarifa por defecto")
    void copia_cada_politica_y_la_tarifa_por_defecto() {
        PlatformBillingConfigDto dto = PlatformBillingConfigDto.from(configurada());

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.defaultPriceList())
                .isEqualTo(new PriceListSummaryDto(7L, "LISTA-2026-01", "Tarifa 2026"));
        assertThat(dto.defaultGraceDays()).isEqualTo(5);
        assertThat(dto.defaultTrialDays()).isEqualTo(14);
        assertThat(dto.invoiceDayOfMonth()).isEqualTo(1);
        assertThat(dto.defaultPaymentTermDays()).isEqualTo(5);
        assertThat(dto.externalBillingProvider()).isEqualTo("SIIGO");
        assertThat(dto.createdDate()).isEqualTo(CREADA);
    }

    @Test
    @DisplayName("sin tarifa por defecto deja el resumen en null, no en un objeto vacío")
    void sin_tarifa_por_defecto_deja_el_resumen_en_null() {
        assertThat(PlatformBillingConfigDto.from(sinTarifa()).defaultPriceList()).isNull();
    }
}
