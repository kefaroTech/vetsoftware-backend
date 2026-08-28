package com.vetsoftware.app.accountingexport.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingexport.application.command.RejectAccountingExportCommand;
import com.vetsoftware.app.accountingexport.application.dto.AccountingExportDto;
import com.vetsoftware.app.accountingexport.application.port.out.AccountingExportRepository;
import com.vetsoftware.app.accountingexport.domain.AccountingExportAlreadyResolvedException;
import com.vetsoftware.app.accountingexport.domain.AccountingExportNotFoundException;
import com.vetsoftware.app.accountingexport.domain.AccountingExportStatus;
import com.vetsoftware.app.accountingexport.testsupport.AccountingExportMother;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Los tres desenlaces de una exportacion: entregada, rechazada o reemplazada.
 * Los tres pasan por leer-modificar-guardar, y ninguno depende de otro puerto
 * mas que el repositorio y el reloj inyectado.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResolveAccountingExportService")
class ResolveAccountingExportServiceTest {

    private static final LocalDateTime AHORA = AccountingExportMother.GENERATED_AT.plusDays(3);

    /** El {@code Clock} no es un puerto: se inyecta de verdad y fijo. */
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @Mock
    private AccountingExportRepository repository;

    private ResolveAccountingExportService service;

    @BeforeEach
    void servicio() {
        service = new ResolveAccountingExportService(repository, RELOJ);
    }

    @Nested
    @DisplayName("markDelivered")
    class MarkDelivered {

        @Test
        @DisplayName("entrega la exportacion GENERATED con la fecha del reloj inyectado")
        void entrega_la_exportacion_generated_con_la_fecha_del_reloj() {
            when(repository.findById(AccountingExportMother.EXPORT_ID))
                    .thenReturn(Optional.of(AccountingExportMother.generado()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AccountingExportDto dto = service.markDelivered(AccountingExportMother.EXPORT_ID);

            assertThat(dto.status()).isEqualTo(AccountingExportStatus.DELIVERED);
            assertThat(dto.deliveredAt()).isEqualTo(AHORA);
            assertThat(dto.rejectedAt()).isNull();
        }

        @ParameterizedTest
        @EnumSource(value = AccountingExportStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "GENERATED")
        @DisplayName("una exportacion que no esta GENERATED rechaza un nuevo desenlace")
        void una_exportacion_que_no_esta_generated_rechaza_un_nuevo_desenlace(
                AccountingExportStatus status) {
            when(repository.findById(AccountingExportMother.EXPORT_ID))
                    .thenReturn(Optional.of(AccountingExportMother.paraEstado(status)));

            assertThatThrownBy(() -> service.markDelivered(AccountingExportMother.EXPORT_ID))
                    .isInstanceOf(AccountingExportAlreadyResolvedException.class)
                    .hasMessageContaining("is already resolved with status " + status);

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una exportacion inexistente lanza AccountingExportNotFoundException")
        void una_exportacion_inexistente_lanza_not_found() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markDelivered(999L))
                    .isInstanceOf(AccountingExportNotFoundException.class)
                    .hasMessageContaining("Accounting export not found: 999");

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markRejected")
    class MarkRejected {

        @Test
        @DisplayName("rechaza la exportacion GENERATED con motivo y la fecha del reloj")
        void rechaza_la_exportacion_generated_con_motivo() {
            when(repository.findById(AccountingExportMother.EXPORT_ID))
                    .thenReturn(Optional.of(AccountingExportMother.generado()));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AccountingExportDto dto = service.markRejected(new RejectAccountingExportCommand(
                    AccountingExportMother.EXPORT_ID, "Totales no cuadran"));

            assertThat(dto.status()).isEqualTo(AccountingExportStatus.REJECTED);
            assertThat(dto.rejectedAt()).isEqualTo(AHORA);
            assertThat(dto.rejectionReason()).isEqualTo("Totales no cuadran");
        }

        @ParameterizedTest
        @EnumSource(value = AccountingExportStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "GENERATED")
        @DisplayName("una exportacion que no esta GENERATED rechaza el rechazo")
        void una_exportacion_que_no_esta_generated_rechaza_el_rechazo(
                AccountingExportStatus status) {
            when(repository.findById(AccountingExportMother.EXPORT_ID))
                    .thenReturn(Optional.of(AccountingExportMother.paraEstado(status)));

            assertThatThrownBy(() -> service.markRejected(
                    new RejectAccountingExportCommand(AccountingExportMother.EXPORT_ID, "motivo")))
                    .isInstanceOf(AccountingExportAlreadyResolvedException.class);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markSuperseded")
    class MarkSuperseded {

        @Test
        @DisplayName("reemplaza una exportacion DELIVERED y le borra la fecha de entrega")
        void reemplaza_una_exportacion_delivered_y_borra_la_fecha_de_entrega() {
            when(repository.findById(AccountingExportMother.EXPORT_ID)).thenReturn(Optional
                    .of(AccountingExportMother.entregado(AccountingExportMother.GENERATED_AT)));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AccountingExportDto dto = service.markSuperseded(AccountingExportMother.EXPORT_ID);

            assertThat(dto.status()).isEqualTo(AccountingExportStatus.SUPERSEDED);
            assertThat(dto.deliveredAt()).isNull();
        }

        @Test
        @DisplayName("una exportacion ya reemplazada rechaza un segundo reemplazo")
        void una_exportacion_ya_reemplazada_rechaza_un_segundo_reemplazo() {
            when(repository.findById(AccountingExportMother.EXPORT_ID))
                    .thenReturn(Optional.of(AccountingExportMother.reemplazado()));

            assertThatThrownBy(() -> service.markSuperseded(AccountingExportMother.EXPORT_ID))
                    .isInstanceOf(AccountingExportAlreadyResolvedException.class);

            verify(repository, never()).save(any());
        }
    }
}
