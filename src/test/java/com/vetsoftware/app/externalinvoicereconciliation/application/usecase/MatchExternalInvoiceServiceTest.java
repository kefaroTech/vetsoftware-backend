package com.vetsoftware.app.externalinvoicereconciliation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicereconciliation.application.command.MatchExternalInvoiceCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.ExternalInvoiceReconciliationRepository;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceAlreadyMatchedException;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationNotFoundException;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import com.vetsoftware.app.externalinvoicereconciliation.testsupport.ExternalInvoiceReconciliationMother;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>Lo que este servicio NO hace es lo que hay que congelar.</b> No clasifica.
 *
 * <p>
 * La decision entre {@code MATCHED}, {@code WITHIN_TOLERANCE} y
 * {@code MISMATCH} vive en la entidad, y este caso lo comprueba por el unico
 * camino que lo demuestra de verdad: pasando los mismos bordes de la tolerancia
 * <b>a traves del servicio</b> y viendo salir el estado correcto sin que el
 * command lo mencione. Si manana alguien moviera la clasificacion aqui, el test
 * seguiria verde —y por eso ademas se comprueba que el command no tiene ningun
 * componente de estado que se pueda enviar—.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MatchExternalInvoiceService — el estado lo decide el dominio, no el servicio")
class MatchExternalInvoiceServiceTest {

    private static final Long ID = 41L;

    @Mock
    private ExternalInvoiceReconciliationRepository repository;

    private MatchExternalInvoiceService service;

    @BeforeEach
    void servicio() {
        service = new MatchExternalInvoiceService(repository);
    }

    @Nested
    @DisplayName("Clasificacion")
    class Clasificacion {

        @ParameterizedTest(name = "externo {0} → diferencia {1} → {2}")
        @CsvSource({"119000.00, 0.00, MATCHED", "118998.00, 2.00, WITHIN_TOLERANCE",
                "119002.00, -2.00, WITHIN_TOLERANCE", "118997.99, 2.01, MISMATCH",
                "119002.01, -2.01, MISMATCH"})
        @DisplayName("devuelve el estado que decidio el dominio, sin recibirlo en el command")
        void devuelve_el_estado_que_decidio_el_dominio(BigDecimal totalExterno,
                BigDecimal diferenciaEsperada, ExternalInvoiceReconciliationStatus esperado) {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(ExternalInvoiceReconciliationMother.abiertaConId(ID)));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            ExternalInvoiceReconciliationDto conciliada = service.execute(comando(totalExterno));

            assertThat(conciliada.status()).isEqualTo(esperado);
            assertThat(conciliada.difference()).isEqualByComparingTo(diferenciaEsperada);
            assertThat(conciliada.externalTotal()).isEqualByComparingTo(totalExterno);
        }

        @Test
        @DisplayName("el command no tiene ningun componente de estado que alguien pueda enviar")
        void el_command_no_tiene_componente_de_estado() {
            // La forma de que el llamante no pueda elegir el estado es que el tipo no
            // lo admita. Este caso se pone rojo el dia que alguien lo anada «para
            // poder forzarlo desde una importacion».
            assertThat(MatchExternalInvoiceCommand.class.getRecordComponents())
                    .extracting(java.lang.reflect.RecordComponent::getName)
                    .doesNotContain("status", "difference");
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una conciliacion que no existe sale como no encontrada y no escribe")
        void una_conciliacion_que_no_existe_no_escribe() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando(new BigDecimal("119000.00"))))
                    .isInstanceOf(ExternalInvoiceReconciliationNotFoundException.class)
                    .hasMessage("External invoice reconciliation not found: 41");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("registrar la factura sobre una ya conciliada sale como conflicto y no escribe")
        void registrar_sobre_una_ya_conciliada_no_escribe() {
            // Sobreescribir la pareja externa borraria el numero con el que alguien ya
            // explico un descuadre y dejaria una difference que no corresponde a
            // ninguna factura conocida.
            when(repository.findById(ID)).thenReturn(Optional.of(ExternalInvoiceReconciliationMother
                    .conFacturaExterna(ID, new BigDecimal("119000.00"))));

            assertThatThrownBy(() -> service.execute(comando(new BigDecimal("50.00"))))
                    .isInstanceOf(ExternalInvoiceAlreadyMatchedException.class)
                    .hasMessageContaining("reconciliation 41");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un total externo negativo no llega a escribirse")
        void un_total_externo_negativo_no_llega_a_escribirse() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(ExternalInvoiceReconciliationMother.abiertaConId(ID)));

            assertThatThrownBy(() -> service.execute(comando(new BigDecimal("-1.00"))))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("externalTotal cannot be negative");

            verify(repository, never()).save(any());
        }
    }

    private static MatchExternalInvoiceCommand comando(BigDecimal totalExterno) {
        return new MatchExternalInvoiceCommand(ID, "FE-1043", "CUFE-0011", totalExterno,
                new BigDecimal("19000.00"), null, null, null, null);
    }
}
