package com.vetsoftware.app.platformbillingconfig.application.usecase;

import static com.vetsoftware.app.platformbillingconfig.testsupport.PlatformBillingConfigMother.TARIFA;
import static com.vetsoftware.app.platformbillingconfig.testsupport.PlatformBillingConfigMother.configurada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.platformbillingconfig.application.command.UpdatePlatformBillingConfigCommand;
import com.vetsoftware.app.platformbillingconfig.application.port.out.PlatformBillingConfigRepository;
import com.vetsoftware.app.platformbillingconfig.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfig;
import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfigNotConfiguredException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdatePlatformBillingConfigService — cambio de políticas")
class UpdatePlatformBillingConfigServiceTest {

    @Mock
    private PlatformBillingConfigRepository repository;
    @Mock
    private PriceListQueryPort priceListQueryPort;

    @InjectMocks
    private UpdatePlatformBillingConfigService service;

    private static UpdatePlatformBillingConfigCommand comando(Long priceListId) {
        return new UpdatePlatformBillingConfigCommand(priceListId, 10, 30, 15, 0, "ALEGRA");
    }

    @Nested
    @DisplayName("Actualización")
    class Actualizacion {

        @Test
        @DisplayName("guarda las políticas nuevas sobre la fila existente")
        void guarda_las_politicas_nuevas_sobre_la_fila_existente() {
            when(repository.find()).thenReturn(Optional.of(configurada()));
            when(priceListQueryPort.findById(7L)).thenReturn(Optional.of(TARIFA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando(7L));

            ArgumentCaptor<PlatformBillingConfig> guardado = ArgumentCaptor
                    .forClass(PlatformBillingConfig.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getDefaultGraceDays()).isEqualTo(10);
            assertThat(guardado.getValue().getDefaultTrialDays()).isEqualTo(30);
            assertThat(guardado.getValue().getInvoiceDayOfMonth()).isEqualTo(15);
            assertThat(guardado.getValue().getDefaultPaymentTermDays()).isZero();
            assertThat(guardado.getValue().getExternalBillingProvider()).isEqualTo("ALEGRA");
            assertThat(guardado.getValue().getDefaultPriceList()).isEqualTo(TARIFA);
        }

        @Test
        @DisplayName("conserva el id de la fila: no crea una segunda configuración")
        void conserva_el_id_de_la_fila() {
            when(repository.find()).thenReturn(Optional.of(configurada()));
            when(priceListQueryPort.findById(7L)).thenReturn(Optional.of(TARIFA));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.execute(comando(7L)).id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("quitar la tarifa por defecto es válido y no consulta el puerto de tarifas")
        void quitar_la_tarifa_por_defecto_es_valido() {
            when(repository.find()).thenReturn(Optional.of(configurada()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThat(service.execute(comando(null)).defaultPriceList()).isNull();

            verifyNoInteractions(priceListQueryPort);
        }
    }

    @Nested
    @DisplayName("Sin la fila sembrada")
    class SinLaFilaSembrada {

        @Test
        @DisplayName("falla igual que la lectura en vez de crearla con un upsert")
        void falla_igual_que_la_lectura_en_vez_de_crearla() {
            when(repository.find()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(7L)))
                    .isInstanceOf(PlatformBillingConfigNotConfiguredException.class)
                    .hasMessageContaining("INSERT INTO platform_billing_config");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("no guarda nada si la tarifa apuntada no existe")
        void no_guarda_nada_si_la_tarifa_apuntada_no_existe() {
            when(repository.find()).thenReturn(Optional.of(configurada()));
            when(priceListQueryPort.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(99L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Price list not found: 99");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("no guarda nada si el día de emisión no existe en todos los meses")
        void no_guarda_nada_si_el_dia_de_emision_no_existe_todos_los_meses() {
            when(repository.find()).thenReturn(Optional.of(configurada()));

            assertThatThrownBy(() -> service
                    .execute(new UpdatePlatformBillingConfigCommand(null, 10, 30, 31, 0, "ALEGRA")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("invoiceDayOfMonth must be between 1 and 28");

            verify(repository, never()).save(any());
        }
    }
}
