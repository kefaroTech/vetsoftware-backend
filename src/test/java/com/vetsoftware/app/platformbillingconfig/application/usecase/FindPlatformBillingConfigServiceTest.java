package com.vetsoftware.app.platformbillingconfig.application.usecase;

import static com.vetsoftware.app.platformbillingconfig.testsupport.PlatformBillingConfigMother.configurada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.platformbillingconfig.application.dto.PlatformBillingConfigDto;
import com.vetsoftware.app.platformbillingconfig.application.port.out.PlatformBillingConfigRepository;
import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfigNotConfiguredException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FindPlatformBillingConfigService — lectura de la fila única")
class FindPlatformBillingConfigServiceTest {

    @Mock
    private PlatformBillingConfigRepository repository;

    @InjectMocks
    private FindPlatformBillingConfigService service;

    @Nested
    @DisplayName("Con la fila sembrada")
    class ConLaFilaSembrada {

        @Test
        @DisplayName("devuelve las políticas tal como están en la base")
        void devuelve_las_politicas_tal_como_estan_en_la_base() {
            when(repository.find()).thenReturn(Optional.of(configurada()));

            PlatformBillingConfigDto dto = service.find();

            assertThat(dto.defaultGraceDays()).isEqualTo(5);
            assertThat(dto.defaultTrialDays()).isEqualTo(14);
            assertThat(dto.invoiceDayOfMonth()).isEqualTo(1);
            assertThat(dto.defaultPaymentTermDays()).isEqualTo(5);
            assertThat(dto.externalBillingProvider()).isEqualTo("SIIGO");
            assertThat(dto.defaultPriceList().code()).isEqualTo("LISTA-2026-01");
        }
    }

    @Nested
    @DisplayName("Sin la fila sembrada")
    class SinLaFilaSembrada {

        @Test
        @DisplayName("falla en vez de devolver vacío en silencio")
        void falla_en_vez_de_devolver_vacio_en_silencio() {
            when(repository.find()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.find())
                    .isInstanceOf(PlatformBillingConfigNotConfiguredException.class);
        }

        @Test
        @DisplayName("el mensaje nombra la tabla y trae el INSERT que la arregla")
        void el_mensaje_nombra_la_tabla_y_trae_el_insert_que_la_arregla() {
            when(repository.find()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.find()).hasMessageContaining("platform_billing_config")
                    .hasMessageContaining("INSERT INTO platform_billing_config")
                    .hasMessageContaining("días de gracia");
        }
    }
}
