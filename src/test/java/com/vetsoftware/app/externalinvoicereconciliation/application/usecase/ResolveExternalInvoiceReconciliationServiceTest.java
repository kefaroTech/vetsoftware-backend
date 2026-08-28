package com.vetsoftware.app.externalinvoicereconciliation.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.externalinvoicereconciliation.application.command.ResolveExternalInvoiceReconciliationCommand;
import com.vetsoftware.app.externalinvoicereconciliation.application.dto.ExternalInvoiceReconciliationDto;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.ExternalInvoiceReconciliationRepository;
import com.vetsoftware.app.externalinvoicereconciliation.application.port.out.SystemUserValidationPort;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationAlreadyResolvedException;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationNotFoundException;
import com.vetsoftware.app.externalinvoicereconciliation.domain.ExternalInvoiceReconciliationStatus;
import com.vetsoftware.app.externalinvoicereconciliation.testsupport.ExternalInvoiceReconciliationMother;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>El instante de la resolucion sale del reloj inyectado, y ese es el caso
 * que de verdad protege algo.</b>
 *
 * <p>
 * {@code resolvedAt} es la mitad del par que decide en que cierre contable
 * queda el ajuste —la otra mitad es {@code postingPeriod}—. Si la fecha la
 * escribiera quien resuelve, un descuadre de abril se podria antedatar a marzo
 * y colarse en un periodo ya cerrado sin que nada lo impidiera: el
 * {@code CHECK} solo mira el formato, y no existe ninguna FK contra
 * {@code accounting_periods} que pueda decir que ese periodo esta cerrado,
 * porque esa tabla no existe.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResolveExternalInvoiceReconciliationService — el cierre del expediente")
class ResolveExternalInvoiceReconciliationServiceTest {

    private static final Long ID = 41L;

    /** Un instante distinto del que trae cualquier escenario del Mother. */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-04-11T09:20:45Z"),
            ZoneOffset.UTC);

    @Mock
    private ExternalInvoiceReconciliationRepository repository;
    @Mock
    private SystemUserValidationPort systemUserValidationPort;

    private ResolveExternalInvoiceReconciliationService service;

    @BeforeEach
    void servicio() {
        service = new ResolveExternalInvoiceReconciliationService(repository,
                systemUserValidationPort, RELOJ);
    }

    @Nested
    @DisplayName("Cierre")
    class Cierre {

        @Test
        @DisplayName("sella la resolucion con la fecha del reloj, no con ninguna que venga del command")
        void sella_la_resolucion_con_la_fecha_del_reloj() {
            firmaValida();
            when(repository.findById(ID)).thenReturn(Optional.of(ExternalInvoiceReconciliationMother
                    .conFacturaExterna(ID, new BigDecimal("118998.00"))));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            ExternalInvoiceReconciliationDto resuelta = service.execute(comando("2026-03"));

            assertThat(resuelta.resolvedAt()).isEqualTo(LocalDateTime.of(2026, 4, 11, 9, 20, 45));
            assertThat(resuelta.resolvedBySystemUserId()).isEqualTo(990L);
            assertThat(resuelta.resolutionNote()).isEqualTo("Diferencia por redondeo del impuesto");
            assertThat(resuelta.postingPeriod()).isEqualTo("2026-03");
            // Resolver explica el descuadre, no lo hace desaparecer: el estado se queda
            // donde estaba. Si se borrara, el historico perderia por que se abrio.
            assertThat(resuelta.status())
                    .isEqualTo(ExternalInvoiceReconciliationStatus.WITHIN_TOLERANCE);
        }

        @Test
        @DisplayName("una MISSING_EXTERNAL se puede cerrar y sigue siendo MISSING_EXTERNAL")
        void una_missing_external_se_puede_cerrar() {
            firmaValida();
            when(repository.findById(ID))
                    .thenReturn(Optional.of(ExternalInvoiceReconciliationMother.abiertaConId(ID)));
            when(repository.save(any())).thenAnswer(llamada -> llamada.getArgument(0));

            ExternalInvoiceReconciliationDto resuelta = service.execute(comando("2026-03"));

            assertThat(resuelta.status())
                    .isEqualTo(ExternalInvoiceReconciliationStatus.MISSING_EXTERNAL);
            assertThat(resuelta.resolvedAt()).isNotNull();
            assertThat(resuelta.externalInvoiceId()).isNull();
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una conciliacion que no existe sale como no encontrada y no escribe")
        void una_conciliacion_que_no_existe_no_escribe() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando("2026-03")))
                    .isInstanceOf(ExternalInvoiceReconciliationNotFoundException.class)
                    .hasMessage("External invoice reconciliation not found: 41");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una firma que no existe no cierra nada")
        void una_firma_que_no_existe_no_cierra_nada() {
            // Sin este paso, fk_eir_resolved_by rechazaria la escritura al final de la
            // transaccion, cuando ya no se puede decir quien falta.
            when(repository.findById(ID))
                    .thenReturn(Optional.of(ExternalInvoiceReconciliationMother.abiertaConId(ID)));
            when(systemUserValidationPort.existsById(990L)).thenReturn(false);

            assertThatThrownBy(() -> service.execute(comando("2026-03")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("System user not found: 990");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un periodo contable con mes 13 no cierra nada")
        void un_periodo_contable_con_mes_13_no_cierra_nada() {
            firmaValida();
            when(repository.findById(ID))
                    .thenReturn(Optional.of(ExternalInvoiceReconciliationMother.abiertaConId(ID)));

            assertThatThrownBy(() -> service.execute(comando("2026-13")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("postingPeriod must be YYYY-MM with month 01..12");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("cerrar dos veces sale como conflicto y no reescribe el periodo")
        void cerrar_dos_veces_no_reescribe_el_periodo() {
            firmaValida();
            when(repository.findById(ID)).thenReturn(Optional.of(
                    ExternalInvoiceReconciliationMother.resuelta(ID, new BigDecimal("119000.00"))));

            assertThatThrownBy(() -> service.execute(comando("2026-04")))
                    .isInstanceOf(ExternalInvoiceReconciliationAlreadyResolvedException.class)
                    .hasMessageContaining("was already resolved");

            verify(repository, never()).save(any());
        }
    }

    private void firmaValida() {
        when(systemUserValidationPort.existsById(990L)).thenReturn(true);
    }

    private static ResolveExternalInvoiceReconciliationCommand comando(String periodo) {
        return new ResolveExternalInvoiceReconciliationCommand(ID,
                ExternalInvoiceReconciliationMother.FIRMANTE,
                "Diferencia por redondeo del impuesto", periodo);
    }
}
