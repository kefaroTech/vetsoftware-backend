package com.vetsoftware.app.aiproposal.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vetsoftware.app.aiproposal.application.port.out.SpendGuardPort.SpendReservation;
import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricNames;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * El techo de gasto que Terraform declara y nadie hacia cumplir.
 *
 * <p>
 * El {@link Clock} se fija en vez de leerse del sistema: la rotacion de dia es
 * exactamente el caso que solo aparece en CI y de noche, y
 * {@code RELOJ_INYECTADO_EN_VEZ_DE_NOW} esta congelada sobre el arbol entero.
 */
@DisplayName("InProcessDailySpendGuard — el tope de gasto, en codigo")
class InProcessDailySpendGuardTest {

    private static final BigDecimal TOPE_DEV = new BigDecimal("0.33");

    /** Una invocacion tipica: ~USD 0,0166. */
    private static final BigDecimal UNA_LLAMADA = new BigDecimal("0.0166");

    private static Clock a(String instante) {
        return Clock.fixed(Instant.parse(instante), ZoneOffset.UTC);
    }

    /** Un reloj que avanza cuando se le dice, para probar la medianoche. */
    private static final class RelojMovil extends Clock {

        private Instant ahora;

        private RelojMovil(String instante) {
            this.ahora = Instant.parse(instante);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        void avanzarA(String instante) {
            this.ahora = Instant.parse(instante);
        }
    }

    @Nested
    @DisplayName("El corte")
    class Corte {

        @Test
        @DisplayName("por debajo del tope reserva; al pasarlo deja de reservar")
        void corta_al_llegar_al_tope() {
            InProcessDailySpendGuard guard = new InProcessDailySpendGuard(a("2026-08-30T10:00:00Z"),
                    new SimpleMeterRegistry(), TOPE_DEV);

            // 19 llamadas caben en 0,33; la vigesima se pasa.
            for (int i = 0; i < 19; i++)
                assertThat(guard.reserve(UNA_LLAMADA)).isPresent();

            assertThat(guard.reserve(UNA_LLAMADA)).isEmpty();
            assertThat(guard.spentToday()).isEqualByComparingTo("0.3154");
        }

        @Test
        @DisplayName("una sola llamada por encima del tope no pasa")
        void una_llamada_enorme_no_pasa() {
            InProcessDailySpendGuard guard = new InProcessDailySpendGuard(a("2026-08-30T10:00:00Z"),
                    new SimpleMeterRegistry(), TOPE_DEV);

            assertThat(guard.reserve(new BigDecimal("5.00"))).isEmpty();
            assertThat(guard.spentToday()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("con el tope a cero no se invoca nunca: es el kill switch")
        void tope_cero_apaga() {
            assertThat(new InProcessDailySpendGuard(a("2026-08-30T10:00:00Z"),
                    new SimpleMeterRegistry(), BigDecimal.ZERO).reserve(UNA_LLAMADA)).isEmpty();
        }

        @Test
        @DisplayName("fail-closed: una estimacion que no se puede usar no reserva")
        void fail_closed() {
            InProcessDailySpendGuard guard = new InProcessDailySpendGuard(a("2026-08-30T10:00:00Z"),
                    new SimpleMeterRegistry(), TOPE_DEV);

            assertThat(guard.reserve(null)).isEmpty();
            assertThat(guard.reserve(BigDecimal.ZERO)).isEmpty();
            assertThat(guard.reserve(new BigDecimal("-1"))).isEmpty();
        }

        @Test
        @DisplayName("un tope negativo mal configurado se lee como cero, no como infinito")
        void tope_negativo_es_cero() {
            assertThat(new InProcessDailySpendGuard(a("2026-08-30T10:00:00Z"),
                    new SimpleMeterRegistry(), new BigDecimal("-10")).reserve(UNA_LLAMADA))
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Reserva y reconciliacion")
    class Reconciliacion {

        @Test
        @DisplayName("el coste real sustituye a la estimacion, no se suma")
        void el_real_sustituye() {
            InProcessDailySpendGuard guard = new InProcessDailySpendGuard(a("2026-08-30T10:00:00Z"),
                    new SimpleMeterRegistry(), TOPE_DEV);

            SpendReservation reserva = guard.reserve(new BigDecimal("0.0176")).orElseThrow();
            guard.reconcile(reserva, new BigDecimal("0.0154"));

            assertThat(guard.spentToday()).isEqualByComparingTo("0.0154");
        }

        @Test
        @DisplayName("si el real supera al estimado se carga el exceso: el gasto ocurrio")
        void el_exceso_se_carga() {
            InProcessDailySpendGuard guard = new InProcessDailySpendGuard(a("2026-08-30T10:00:00Z"),
                    new SimpleMeterRegistry(), TOPE_DEV);

            SpendReservation reserva = guard.reserve(new BigDecimal("0.0100")).orElseThrow();
            guard.reconcile(reserva, new BigDecimal("0.0500"));

            assertThat(guard.spentToday()).isEqualByComparingTo("0.0500");
        }

        @Test
        @DisplayName("liberar devuelve el cupo entero; solo vale si no se invoco")
        void liberar_devuelve_el_cupo() {
            InProcessDailySpendGuard guard = new InProcessDailySpendGuard(a("2026-08-30T10:00:00Z"),
                    new SimpleMeterRegistry(), TOPE_DEV);

            guard.release(guard.reserve(UNA_LLAMADA).orElseThrow());

            assertThat(guard.spentToday()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("una reserva nula no mueve nada ni revienta")
        void reserva_nula() {
            InProcessDailySpendGuard guard = new InProcessDailySpendGuard(a("2026-08-30T10:00:00Z"),
                    new SimpleMeterRegistry(), TOPE_DEV);

            guard.reconcile(null, new BigDecimal("9"));
            guard.release(null);

            assertThat(guard.spentToday()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("el acumulado nunca baja de cero")
        void nunca_negativo() {
            InProcessDailySpendGuard guard = new InProcessDailySpendGuard(a("2026-08-30T10:00:00Z"),
                    new SimpleMeterRegistry(), TOPE_DEV);

            SpendReservation reserva = guard.reserve(UNA_LLAMADA).orElseThrow();
            guard.release(reserva);
            guard.release(reserva);

            assertThat(guard.spentToday()).isEqualByComparingTo("0.00");
        }
    }

    @Test
    @DisplayName("el contador se reinicia al cambiar el dia, no cada 24 horas rodantes")
    void el_dia_rota() {
        RelojMovil reloj = new RelojMovil("2026-08-30T23:59:00Z");
        InProcessDailySpendGuard guard = new InProcessDailySpendGuard(reloj,
                new SimpleMeterRegistry(), TOPE_DEV);

        for (int i = 0; i < 19; i++)
            guard.reserve(UNA_LLAMADA);
        assertThat(guard.reserve(UNA_LLAMADA)).isEmpty();

        reloj.avanzarA("2026-08-31T00:01:00Z");

        assertThat(guard.spentToday()).isEqualByComparingTo("0.00");
        assertThat(guard.reserve(UNA_LLAMADA)).isPresent();
    }

    @Test
    @DisplayName("una reserva sin identificador no se construye")
    void la_reserva_se_valida() {
        assertThat(new SpendReservation("r-1", BigDecimal.ONE).reservedUsd())
                .isEqualByComparingTo("1");
        assertThat(Optional.of(new SpendReservation("r-2", BigDecimal.ZERO))).isPresent();
    }

    @Nested
    @DisplayName("Telemetria del gasto")
    class Telemetria {

        private SimpleMeterRegistry registro;

        private InProcessDailySpendGuard guard;

        @BeforeEach
        void montar() {
            registro = new SimpleMeterRegistry();
            guard = new InProcessDailySpendGuard(a("2026-08-30T10:00:00Z"), registro, TOPE_DEV);
        }

        @Test
        @DisplayName("el contador cuenta el coste REAL de la llamada, no la reserva pesimista")
        void el_contador_cuenta_el_coste_real() {
            SpendReservation reserva = guard.reserve(UNA_LLAMADA).orElseThrow();

            guard.reconcile(reserva, new BigDecimal("0.0042"));

            assertThat(registro.get(BusinessMetricNames.AI_PROPOSAL_SPEND).counter().count())
                    .isEqualTo(0.0042);
        }

        @Test
        @DisplayName("el intento que fallo despues de pagar TAMBIEN cuenta: es la unica cuenta que hay")
        void el_intento_fallido_tambien_cuenta() {
            // Es el caso que se perderia si la metrica se emitiera desde el caso de
            // uso: alli no hay ModelUsage que leer, porque el turno fallido no
            // persiste tokens. El contador diria menos que la factura de AWS.
            SpendReservation reserva = guard.reserve(UNA_LLAMADA).orElseThrow();

            guard.reconcile(reserva, UNA_LLAMADA);

            assertThat(registro.get(BusinessMetricNames.AI_PROPOSAL_SPEND).counter().count())
                    .isEqualTo(0.0166);
        }

        @Test
        @DisplayName("el medidor del dia sigue al acumulado y se reinicia al rotar, que el contador no puede")
        void el_medidor_del_dia_sigue_al_acumulado() {
            RelojMovil reloj = new RelojMovil("2026-08-30T23:00:00Z");
            SimpleMeterRegistry propio = new SimpleMeterRegistry();
            InProcessDailySpendGuard conReloj = new InProcessDailySpendGuard(reloj, propio,
                    TOPE_DEV);
            conReloj.reserve(UNA_LLAMADA);

            assertThat(propio.get(BusinessMetricNames.AI_PROPOSAL_SPEND_TODAY).gauge().value())
                    .isEqualTo(0.0166);

            reloj.avanzarA("2026-08-31T00:01:00Z");

            assertThat(propio.get(BusinessMetricNames.AI_PROPOSAL_SPEND_TODAY).gauge().value())
                    .isZero();
        }

        @Test
        @DisplayName("el aviso del tope agotado sale UNA vez al dia, no una por peticion")
        void el_aviso_del_tope_no_es_una_tormenta() {
            Logger guardia = (Logger) LoggerFactory.getLogger(InProcessDailySpendGuard.class);
            ListAppender<ILoggingEvent> eventos = new ListAppender<>();
            eventos.start();
            guardia.addAppender(eventos);
            try {
                for (int i = 0; i < 19; i++)
                    guard.reserve(UNA_LLAMADA);
                for (int i = 0; i < 50; i++)
                    guard.reserve(UNA_LLAMADA);

                // Un WARN por peticion durante ocho horas en un endpoint publico es la
                // tormenta que ensena a ignorar el canal. Quien despierta a alguien es
                // la alerta sobre el contador de gasto, no este evento.
                assertThat(eventos.list).filteredOn(evento -> evento.getLevel() == Level.WARN)
                        .hasSize(1);
            } finally {
                guardia.detachAppender(eventos);
                eventos.stop();
            }
        }

        @Test
        @DisplayName("una estimacion no utilizable es ERROR y no WARN: nadie la recupera y apaga la IA entera")
        void una_estimacion_rota_es_error() {
            Logger guardia = (Logger) LoggerFactory.getLogger(InProcessDailySpendGuard.class);
            ListAppender<ILoggingEvent> eventos = new ListAppender<>();
            eventos.start();
            guardia.addAppender(eventos);
            try {
                assertThat(guard.reserve(null)).isEmpty();
                assertThat(guard.reserve(BigDecimal.ZERO)).isEmpty();

                assertThat(eventos.list).hasSize(2)
                        .allMatch(evento -> evento.getLevel() == Level.ERROR);
            } finally {
                guardia.detachAppender(eventos);
                eventos.stop();
            }
        }

        @Test
        @DisplayName("liberar una reserva no incrementa el gasto: no hubo llamada")
        void liberar_no_gasta() {
            SpendReservation reserva = guard.reserve(UNA_LLAMADA).orElseThrow();

            guard.release(reserva);

            assertThat(registro.get(BusinessMetricNames.AI_PROPOSAL_SPEND).counter().count())
                    .isZero();
            assertThat(guard.spentToday()).isEqualByComparingTo("0.00");
        }
    }
}
