package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.config.ClockConfig;
import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionChangedEvent;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionStatusUseCase;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionChangedPort;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionRepository;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.Subscription;
import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChangeReason;
import com.vetsoftware.app.subscription.testsupport.SubscriptionMother;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * D-81 · la zona del reloj es quien decide que dia es «hoy».
 *
 * <p>
 * El bean de {@link ClockConfig} devolvia {@code Clock.systemDefaultZone()} y
 * la imagen no declara zona, asi que en produccion el reloj corria en UTC: una
 * prueba gratuita moria a las 19:00 de su ultimo dia en lugar de a la
 * medianoche de Bogota. El arreglo es {@code Clock.system(BUSINESS_ZONE)}.
 *
 * <p>
 * <b>La forma de estas pruebas es lo que las hace utiles</b>: los dos primeros
 * casos alimentan al worker con <b>el mismo instante</b> y solo cambian la zona
 * del {@code Clock.fixed(...)}. Si lo unico que se mueve es la zona y la
 * decision del negocio cambia, queda demostrado que la zona es quien decide, y
 * que devolver el reloj a {@code systemDefaultZone()} reintroduce D-81.
 *
 * <p>
 * No sustituye a {@code SubscriptionLifecycleWorkerTest}, que cubre el
 * recorrido del lote, el cursor y las validaciones: aqui solo se fija el limite
 * del calendario.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionLifecycleWorker — la zona del reloj decide que dia es hoy (D-81)")
class SubscriptionLifecycleWorkerZoneTest {

    /**
     * Ultimo dia de prueba, inclusive: el contrato sigue en TRIALING todo este dia.
     */
    private static final LocalDate ULTIMO_DIA_DE_PRUEBA = LocalDate.of(2026, 3, 15);
    private static final LocalDate DIA_SIGUIENTE = ULTIMO_DIA_DE_PRUEBA.plusDays(1);

    /**
     * Las 19:30 del ultimo dia de prueba en Bogota, como instante inequivoco. Ese
     * mismo instante ya es {@code 2026-03-16T00:30Z}: justo la franja en la que el
     * reloj sin zona adelantaba el calendario un dia.
     */
    private static final Instant LAS_19_30_DEL_ULTIMO_DIA = enBogota(ULTIMO_DIA_DE_PRUEBA,
            LocalTime.of(19, 30));

    private static final Instant LAS_23_59_DEL_ULTIMO_DIA = enBogota(ULTIMO_DIA_DE_PRUEBA,
            LocalTime.of(23, 59));

    private static final Instant LAS_00_01_DEL_DIA_SIGUIENTE = enBogota(DIA_SIGUIENTE,
            LocalTime.of(0, 1));

    private static final long CURSOR = 0L;
    private static final int LOTE = 20;

    @Mock
    private SubscriptionRepository repository;
    @Mock
    private ChangeSubscriptionStatusUseCase changeStatusUseCase;
    @Mock
    private SubscriptionChangedPort subscriptionChangedPort;

    @Nested
    @DisplayName("Las 19:30 del ultimo dia de prueba: un unico instante y dos zonas")
    class ZonaDelReloj {

        @Test
        @DisplayName("con la zona del negocio la prueba gratuita sigue viva a las 19:30")
        void con_la_zona_del_negocio_la_prueba_sigue_viva_a_las_19_30() {
            when(repository.lockLifecycleBatchAfter(CURSOR, LOTE))
                    .thenReturn(List.of(pruebaGratuitaQueVenceEl(ULTIMO_DIA_DE_PRUEBA)));

            workerCon(Clock.fixed(LAS_19_30_DEL_ULTIMO_DIA, ClockConfig.BUSINESS_ZONE))
                    .processBatchAfter(CURSOR, LOTE);

            verify(changeStatusUseCase, never()).execute(any());
            // El evento diario lleva el «hoy» que calculo el worker: demuestra que la
            // fecha de calendario sigue siendo el ultimo dia de prueba, no el siguiente.
            verify(subscriptionChangedPort)
                    .subscriptionChanged(vigenciasDelDia(ULTIMO_DIA_DE_PRUEBA));
        }

        @Test
        @DisplayName("con el reloj en UTC el mismo instante da la prueba por terminada (D-81)")
        void con_el_reloj_en_utc_el_mismo_instante_da_la_prueba_por_terminada() {
            when(repository.lockLifecycleBatchAfter(CURSOR, LOTE))
                    .thenReturn(List.of(pruebaGratuitaQueVenceEl(ULTIMO_DIA_DE_PRUEBA)));

            workerCon(Clock.fixed(LAS_19_30_DEL_ULTIMO_DIA, ZoneOffset.UTC))
                    .processBatchAfter(CURSOR, LOTE);

            assertThat(transicionEjecutada().status()).isEqualTo(SubscriptionStatus.ACTIVE);
            verifyNoInteractions(subscriptionChangedPort);
        }
    }

    @Nested
    @DisplayName("El limite es la medianoche local, no otra hora")
    class MedianocheLocal {

        @Test
        @DisplayName("a las 23:59 del ultimo dia la prueba gratuita todavia no ha terminado")
        void a_las_23_59_del_ultimo_dia_la_prueba_todavia_no_ha_terminado() {
            when(repository.lockLifecycleBatchAfter(CURSOR, LOTE))
                    .thenReturn(List.of(pruebaGratuitaQueVenceEl(ULTIMO_DIA_DE_PRUEBA)));

            workerCon(Clock.fixed(LAS_23_59_DEL_ULTIMO_DIA, ClockConfig.BUSINESS_ZONE))
                    .processBatchAfter(CURSOR, LOTE);

            verify(changeStatusUseCase, never()).execute(any());
            verify(subscriptionChangedPort)
                    .subscriptionChanged(vigenciasDelDia(ULTIMO_DIA_DE_PRUEBA));
        }

        @Test
        @DisplayName("a las 00:01 del dia siguiente la prueba gratuita pasa a ACTIVE")
        void a_las_00_01_del_dia_siguiente_la_prueba_pasa_a_active() {
            when(repository.lockLifecycleBatchAfter(CURSOR, LOTE))
                    .thenReturn(List.of(pruebaGratuitaQueVenceEl(ULTIMO_DIA_DE_PRUEBA)));

            workerCon(Clock.fixed(LAS_00_01_DEL_DIA_SIGUIENTE, ClockConfig.BUSINESS_ZONE))
                    .processBatchAfter(CURSOR, LOTE);

            ChangeSubscriptionStatusCommand command = transicionEjecutada();
            assertThat(command.status()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(command.id()).isEqualTo(SubscriptionMother.CONTRATO);
            assertThat(command.companyId()).isEqualTo(SubscriptionMother.EMPRESA);
            // El motivo dejo de ser una frase con la fecha dentro. La fecha no se
            // pierde: sigue en occurredAt de la fila y en el propio contrato.
            assertThat(command.reason()).isEqualTo(SubscriptionStatusChangeReason.TRIAL_ENDED);
            verifyNoInteractions(subscriptionChangedPort);
        }
    }

    private SubscriptionLifecycleWorker workerCon(Clock clock) {
        return new SubscriptionLifecycleWorker(repository, changeStatusUseCase,
                subscriptionChangedPort, clock);
    }

    /**
     * La unica transicion que el worker pidio, capturada para afirmar sobre ella.
     */
    private ChangeSubscriptionStatusCommand transicionEjecutada() {
        ArgumentCaptor<ChangeSubscriptionStatusCommand> command = ArgumentCaptor
                .forClass(ChangeSubscriptionStatusCommand.class);
        verify(changeStatusUseCase).execute(command.capture());
        return command.getValue();
    }

    private static SubscriptionChangedEvent vigenciasDelDia(LocalDate hoy) {
        return new SubscriptionChangedEvent(SubscriptionMother.EMPRESA, SubscriptionMother.CONTRATO,
                SubscriptionChangeKind.EFFECTIVE_DATE_REACHED, hoy);
    }

    private static Instant enBogota(LocalDate dia, LocalTime hora) {
        return ZonedDateTime.of(dia, hora, ClockConfig.BUSINESS_ZONE).toInstant();
    }

    /**
     * Contrato en TRIALING cuyo ultimo dia de prueba es {@code ultimoDia}. Entidad
     * real y no un mock: un {@code Subscription} doblado no valida sus invariantes
     * y el caso pasaria con datos que produccion rechaza.
     */
    private static Subscription pruebaGratuitaQueVenceEl(LocalDate ultimoDia) {
        return new Subscription(SubscriptionMother.CONTRATO, "SUS-2026-00184",
                SubscriptionMother.EMPRESA, null, 3L, BillingCycle.MONTHLY,
                SubscriptionStatus.TRIALING, LocalDate.of(2026, 3, 1), ultimoDia,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 4, 1), null,
                5, null, true, null, null, 0L, true);
    }
}
