package com.vetsoftware.app.accountingperiod.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingperiod.application.command.LockAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.port.out.AccountingPeriodRepository;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodAlreadyClosedException;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodNotFoundException;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
import com.vetsoftware.app.accountingperiod.domain.LastOpenAccountingPeriodException;
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
 * Declaracion de un mes.
 *
 * <p>
 * <b>Es la unica operacion irreversible del modelo</b>, y por eso la guarda del
 * ultimo periodo abierto importa aqui todavia mas que en el cierre en blando:
 * declarar el unico mes abierto deja al sistema sin donde imputar nada y sin
 * camino de vuelta.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LockAccountingPeriodService — declarar el mes")
class LockAccountingPeriodServiceTest {

    private static final Long ID = 8800L;

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-04-05T22:30:00Z"),
            ZoneId.of("America/Bogota"));

    private static final LocalDateTime CIERRE_ESPERADO = LocalDateTime.of(2026, 4, 5, 17, 30, 0);

    @Mock
    private AccountingPeriodRepository repository;

    private LockAccountingPeriodService service;

    @BeforeEach
    void servicio() {
        service = new LockAccountingPeriodService(repository, RELOJ);
    }

    @Nested
    @DisplayName("Declaracion")
    class Declaracion {

        @Test
        @DisplayName("declarar un mes abierto lo sella con la firma y la hora del reloj")
        void declarar_un_mes_abierto_lo_sella() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(AccountingPeriodMother.persistidoAbierto(ID)));
            when(repository.countOpenExcluding(ID)).thenReturn(3L);
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            service.execute(
                    new LockAccountingPeriodCommand(ID, AccountingPeriodMother.CERRADO_POR));

            ArgumentCaptor<AccountingPeriod> guardado = ArgumentCaptor
                    .forClass(AccountingPeriod.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue()).satisfies(periodo -> {
                assertThat(periodo.getStatus()).isEqualTo(AccountingPeriodStatus.LOCKED);
                assertThat(periodo.getClosedAt()).isEqualTo(CIERRE_ESPERADO);
                assertThat(periodo.getClosedBySystemUserId())
                        .isEqualTo(AccountingPeriodMother.CERRADO_POR);
            });
        }

        @Test
        @DisplayName("declarar un mes ya cerrado conserva la firma del cierre original")
        void declarar_un_mes_ya_cerrado_conserva_la_firma_original() {
            // La tabla guarda un cierre, no una pila: quien declara no borra a quien
            // cerro. Y como el mes ya no estaba abierto, la guarda del ultimo abierto ni
            // se consulta.
            when(repository.findById(ID))
                    .thenReturn(Optional.of(AccountingPeriodMother.cerradoEnBlando(ID)));
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            service.execute(
                    new LockAccountingPeriodCommand(ID, AccountingPeriodMother.REABIERTO_POR));

            ArgumentCaptor<AccountingPeriod> guardado = ArgumentCaptor
                    .forClass(AccountingPeriod.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue()).satisfies(periodo -> {
                assertThat(periodo.getStatus()).isEqualTo(AccountingPeriodStatus.LOCKED);
                assertThat(periodo.getClosedAt()).isEqualTo(AccountingPeriodMother.CERRADO_EL);
                assertThat(periodo.getClosedBySystemUserId())
                        .isEqualTo(AccountingPeriodMother.CERRADO_POR);
            });
            verify(repository, never()).countOpenExcluding(any());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("declarar el ultimo mes abierto se rechaza y NO escribe")
        void declarar_el_ultimo_mes_abierto_se_rechaza() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(AccountingPeriodMother.persistidoAbierto(ID)));
            when(repository.countOpenExcluding(ID)).thenReturn(0L);

            assertThatThrownBy(() -> service.execute(
                    new LockAccountingPeriodCommand(ID, AccountingPeriodMother.CERRADO_POR)))
                    .isInstanceOf(LastOpenAccountingPeriodException.class)
                    .hasMessageContaining("is the last open one");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("declarar dos veces es un conflicto")
        void declarar_dos_veces_es_un_conflicto() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(AccountingPeriodMother.declarado(ID)));

            assertThatThrownBy(() -> service.execute(
                    new LockAccountingPeriodCommand(ID, AccountingPeriodMother.CERRADO_POR)))
                    .isInstanceOf(AccountingPeriodAlreadyClosedException.class)
                    .hasMessageContaining("is already closed with status LOCKED");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un mes inexistente es un 404 y no escribe nada")
        void un_mes_inexistente_es_un_404() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(
                    new LockAccountingPeriodCommand(ID, AccountingPeriodMother.CERRADO_POR)))
                    .isInstanceOf(AccountingPeriodNotFoundException.class)
                    .hasMessageContaining("8800");

            verify(repository, never()).save(any());
        }
    }
}
