package com.vetsoftware.app.accountingperiod.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingperiod.application.command.ReopenAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.application.port.out.AccountingPeriodRepository;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodNotClosedException;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodNotFoundException;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
import com.vetsoftware.app.accountingperiod.domain.LockedAccountingPeriodCannotBeReopenedException;
import com.vetsoftware.app.accountingperiod.testsupport.AccountingPeriodMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Reapertura de un mes cerrado.
 *
 * <p>
 * <b>Aqui no hay guarda de «ultimo periodo abierto» y no falta</b>: reabrir
 * suma un mes abierto, nunca resta. Lo que si se comprueba es que el motivo
 * llega intacto al dominio, porque {@code reopened_reason} es lo unico que un
 * revisor puede leer despues para saber por que se deshizo un cierre.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReopenAccountingPeriodService — reabrir con firma y motivo")
class ReopenAccountingPeriodServiceTest {

    private static final Long ID = 8800L;

    /** 2026-04-09T14:12:45Z, que en Bogota es el dia 9 a las 09:12:45. */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-04-09T14:12:45Z"),
            ZoneId.of("America/Bogota"));

    private static final LocalDateTime REAPERTURA_ESPERADA = LocalDateTime.of(2026, 4, 9, 9, 12,
            45);

    @Mock
    private AccountingPeriodRepository repository;

    private ReopenAccountingPeriodService service;

    @BeforeEach
    void servicio() {
        service = new ReopenAccountingPeriodService(repository, RELOJ);
    }

    @Nested
    @DisplayName("Reapertura")
    class Reapertura {

        @Test
        @DisplayName("guarda el mes abierto de nuevo, con el motivo y conservando el cierre")
        void guarda_el_mes_abierto_con_motivo_y_cierre() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(AccountingPeriodMother.cerradoEnBlando(ID)));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            AccountingPeriodDto reabierto = service.execute(new ReopenAccountingPeriodCommand(ID,
                    AccountingPeriodMother.REABIERTO_POR, AccountingPeriodMother.MOTIVO));

            ArgumentCaptor<AccountingPeriod> guardado = ArgumentCaptor
                    .forClass(AccountingPeriod.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue()).satisfies(periodo -> {
                assertThat(periodo.getStatus()).isEqualTo(AccountingPeriodStatus.OPEN);
                assertThat(periodo.getReopenedAt()).isEqualTo(REAPERTURA_ESPERADA);
                assertThat(periodo.getReopenedBySystemUserId())
                        .isEqualTo(AccountingPeriodMother.REABIERTO_POR);
                assertThat(periodo.getReopenedReason()).isEqualTo(AccountingPeriodMother.MOTIVO);
                assertThat(periodo.getClosedAt()).isEqualTo(AccountingPeriodMother.CERRADO_EL);
                assertThat(periodo.getClosedBySystemUserId())
                        .isEqualTo(AccountingPeriodMother.CERRADO_POR);
            });
            assertThat(reabierto.reopenedReason()).isEqualTo(AccountingPeriodMother.MOTIVO);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un mes LOCKED no se reabre y NO se escribe nada")
        void un_mes_declarado_no_se_reabre() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(AccountingPeriodMother.declarado(ID)));

            assertThatThrownBy(() -> service.execute(new ReopenAccountingPeriodCommand(ID,
                    AccountingPeriodMother.REABIERTO_POR, AccountingPeriodMother.MOTIVO)))
                    .isInstanceOf(LockedAccountingPeriodCannotBeReopenedException.class)
                    .hasMessageContaining("is locked and cannot be reopened");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un mes que ya estaba abierto contesta que no estaba cerrado")
        void un_mes_ya_abierto_contesta_que_no_estaba_cerrado() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(AccountingPeriodMother.persistidoAbierto(ID)));

            assertThatThrownBy(() -> service.execute(new ReopenAccountingPeriodCommand(ID,
                    AccountingPeriodMother.REABIERTO_POR, AccountingPeriodMother.MOTIVO)))
                    .isInstanceOf(AccountingPeriodNotClosedException.class)
                    .hasMessageContaining("is not closed, its status is OPEN");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("sin motivo escrito no se reabre, aunque el binder lo hubiera dejado pasar")
        void sin_motivo_escrito_no_se_reabre() {
            // La red del @NotBlank del request solo cubre la entrada por HTTP. Este caso
            // congela que el dominio tambien lo exige para cualquier otro caller.
            when(repository.findById(ID))
                    .thenReturn(Optional.of(AccountingPeriodMother.cerradoEnBlando(ID)));

            assertThatThrownBy(() -> service.execute(new ReopenAccountingPeriodCommand(ID,
                    AccountingPeriodMother.REABIERTO_POR, "   ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reopenedReason is required");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un mes inexistente es un 404 y no escribe nada")
        void un_mes_inexistente_es_un_404() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(new ReopenAccountingPeriodCommand(ID,
                    AccountingPeriodMother.REABIERTO_POR, AccountingPeriodMother.MOTIVO)))
                    .isInstanceOf(AccountingPeriodNotFoundException.class)
                    .hasMessageContaining("8800");

            verify(repository, never()).save(any());
        }
    }
}
