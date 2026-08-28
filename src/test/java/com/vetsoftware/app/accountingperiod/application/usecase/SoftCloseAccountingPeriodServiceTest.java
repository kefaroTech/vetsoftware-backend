package com.vetsoftware.app.accountingperiod.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingperiod.application.command.SoftCloseAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
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
 * Cierre en blando de un mes.
 *
 * <p>
 * <b>Lo que solo se puede comprobar aqui es la guarda del ultimo periodo
 * abierto</b>: es una invariante del conjunto, no de la fila, asi que ni el
 * dominio ni la rodaja de persistencia la ven.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SoftCloseAccountingPeriodService — cerrar sin quedarse sin meses abiertos")
class SoftCloseAccountingPeriodServiceTest {

    private static final Long ID = 8800L;

    /** 2026-04-05T22:30Z, que en Bogota es el dia 5 a las 17:30. */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-04-05T22:30:00Z"),
            ZoneId.of("America/Bogota"));

    private static final LocalDateTime CIERRE_ESPERADO = LocalDateTime.of(2026, 4, 5, 17, 30, 0);

    @Mock
    private AccountingPeriodRepository repository;

    private SoftCloseAccountingPeriodService service;

    @BeforeEach
    void servicio() {
        service = new SoftCloseAccountingPeriodService(repository, RELOJ);
    }

    @Nested
    @DisplayName("Cierre")
    class Cierre {

        @Test
        @DisplayName("sella el mes con la firma del command y la hora del reloj del negocio")
        void sella_el_mes_con_la_firma_y_la_hora() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(AccountingPeriodMother.persistidoAbierto(ID)));
            when(repository.countOpenExcluding(ID)).thenReturn(2L);
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            AccountingPeriodDto cerrado = service.execute(
                    new SoftCloseAccountingPeriodCommand(ID, AccountingPeriodMother.CERRADO_POR));

            ArgumentCaptor<AccountingPeriod> guardado = ArgumentCaptor
                    .forClass(AccountingPeriod.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue()).satisfies(periodo -> {
                assertThat(periodo.getStatus()).isEqualTo(AccountingPeriodStatus.SOFT_CLOSED);
                assertThat(periodo.getClosedAt()).isEqualTo(CIERRE_ESPERADO);
                assertThat(periodo.getClosedBySystemUserId())
                        .isEqualTo(AccountingPeriodMother.CERRADO_POR);
            });
            assertThat(cerrado.status()).isEqualTo(AccountingPeriodStatus.SOFT_CLOSED);
        }
    }

    @Nested
    @DisplayName("La invariante del ultimo periodo abierto")
    class UltimoPeriodoAbierto {

        @Test
        @DisplayName("cerrar el ultimo mes abierto se rechaza y NO escribe")
        void cerrar_el_ultimo_mes_abierto_se_rechaza() {
            // Sin ningun mes abierto, ResolvePostingPeriodUseCase no tiene donde imputar
            // nada y toda escritura con efecto contable queda rechazada al dia
            // siguiente, en otra feature y sin relacion aparente con este cierre.
            when(repository.findById(ID))
                    .thenReturn(Optional.of(AccountingPeriodMother.persistidoAbierto(ID)));
            when(repository.countOpenExcluding(ID)).thenReturn(0L);

            assertThatThrownBy(() -> service.execute(
                    new SoftCloseAccountingPeriodCommand(ID, AccountingPeriodMother.CERRADO_POR)))
                    .isInstanceOf(LastOpenAccountingPeriodException.class)
                    .hasMessageContaining("is the last open one");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("la cuenta excluye el propio mes: quedan otros abiertos y el cierre sigue")
        void la_cuenta_excluye_el_propio_mes() {
            when(repository.findById(ID))
                    .thenReturn(Optional.of(AccountingPeriodMother.persistidoAbierto(ID)));
            when(repository.countOpenExcluding(ID)).thenReturn(1L);
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            service.execute(
                    new SoftCloseAccountingPeriodCommand(ID, AccountingPeriodMother.CERRADO_POR));

            verify(repository).countOpenExcluding(ID);
            verify(repository).save(any());
        }

        @Test
        @DisplayName("un mes ya cerrado ni siquiera cuenta los abiertos: contesta el dominio")
        void un_mes_ya_cerrado_no_cuenta_los_abiertos() {
            // Preguntar primero por la cuenta daria «es el ultimo abierto» sobre un mes
            // que no lo esta, que es justo el mensaje que manda a buscar donde no es.
            when(repository.findById(ID))
                    .thenReturn(Optional.of(AccountingPeriodMother.cerradoEnBlando(ID)));

            assertThatThrownBy(() -> service.execute(
                    new SoftCloseAccountingPeriodCommand(ID, AccountingPeriodMother.CERRADO_POR)))
                    .isInstanceOf(AccountingPeriodAlreadyClosedException.class)
                    .hasMessageContaining("is already closed with status SOFT_CLOSED");

            verify(repository, never()).countOpenExcluding(any());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un mes inexistente es un 404 y no escribe nada")
        void un_mes_inexistente_es_un_404() {
            when(repository.findById(ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(
                    new SoftCloseAccountingPeriodCommand(ID, AccountingPeriodMother.CERRADO_POR)))
                    .isInstanceOf(AccountingPeriodNotFoundException.class)
                    .hasMessageContaining("8800");

            verify(repository, never()).save(any());
        }
    }
}
