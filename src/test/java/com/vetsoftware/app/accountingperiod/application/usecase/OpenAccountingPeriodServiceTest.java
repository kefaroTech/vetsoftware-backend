package com.vetsoftware.app.accountingperiod.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.accountingperiod.application.command.OpenAccountingPeriodCommand;
import com.vetsoftware.app.accountingperiod.application.dto.AccountingPeriodDto;
import com.vetsoftware.app.accountingperiod.application.port.out.AccountingPeriodRepository;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriod;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodAlreadyExistsException;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodKey;
import com.vetsoftware.app.accountingperiod.domain.AccountingPeriodStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Apertura de un mes contable.
 *
 * <p>
 * <b>El reloj esta fijado en la ultima media hora del mes en UTC</b>, que en
 * Bogota es todavia el dia anterior: es el caso que separa el reloj inyectado
 * de un {@code LocalDateTime.now()} pelado, y {@code created_date} decide en
 * que dia consta que se abrio el mes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAccountingPeriodService — abrir el mes")
class OpenAccountingPeriodServiceTest {

    /** 2026-03-31T23:30Z, que en Bogota es todavia el dia 31 a las 18:30. */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-31T23:30:00Z"),
            ZoneId.of("America/Bogota"));

    private static final LocalDateTime CREACION_ESPERADA = LocalDateTime.of(2026, 3, 31, 18, 30, 0);

    @Mock
    private AccountingPeriodRepository repository;

    private OpenAccountingPeriodService service;

    @BeforeEach
    void servicio() {
        service = new OpenAccountingPeriodService(repository, RELOJ);
    }

    @Nested
    @DisplayName("Apertura")
    class Apertura {

        @Test
        @DisplayName("guarda un mes abierto, sin cierre y fechado con el reloj del negocio")
        void guarda_un_mes_abierto_fechado_con_el_reloj() {
            when(repository.existsByPeriodKey(AccountingPeriodKey.of("2026-04"))).thenReturn(false);
            when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));

            AccountingPeriodDto abierto = service
                    .execute(new OpenAccountingPeriodCommand("2026-04"));

            ArgumentCaptor<AccountingPeriod> guardado = ArgumentCaptor
                    .forClass(AccountingPeriod.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue()).satisfies(periodo -> {
                assertThat(periodo.getPeriodKey().value()).isEqualTo("2026-04");
                assertThat(periodo.getStatus()).isEqualTo(AccountingPeriodStatus.OPEN);
                assertThat(periodo.getClosedAt()).isNull();
                assertThat(periodo.getCreatedDate()).isEqualTo(CREACION_ESPERADA);
            });
            assertThat(abierto.periodKey()).isEqualTo("2026-04");
            assertThat(abierto.status()).isEqualTo(AccountingPeriodStatus.OPEN);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un mes que ya existe es un conflicto y NO se vuelve a guardar")
        void un_mes_que_ya_existe_es_un_conflicto() {
            // Traduce uq_accounting_periods_period a un 409 legible: el cierre mensual
            // disparado dos veces es el escenario normal, y un Duplicate entry del
            // driver no le dice nada a quien lo ve.
            when(repository.existsByPeriodKey(AccountingPeriodKey.of("2026-03"))).thenReturn(true);

            assertThatThrownBy(() -> service.execute(new OpenAccountingPeriodCommand("2026-03")))
                    .isInstanceOf(AccountingPeriodAlreadyExistsException.class)
                    .hasMessageContaining("2026-03");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("una clave mal escrita ni siquiera llega a consultar el repositorio")
        void una_clave_mal_escrita_no_llega_al_repositorio() {
            // El value object valida antes de tocar la base: el mes 13 no existe y
            // preguntar por el seria un viaje inutil.
            assertThatThrownBy(() -> service.execute(new OpenAccountingPeriodCommand("2026-13")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("periodKey must have the form yyyy-MM");

            verifyNoInteractions(repository);
        }
    }
}
