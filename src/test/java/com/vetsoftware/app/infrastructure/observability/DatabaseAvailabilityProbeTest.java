package com.vetsoftware.app.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vetsoftware.app.infrastructure.logging.RedactingAppender;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.KeyValuePair;

/**
 * La sonda reparte el trabajo en tres: la métrica dice el hecho sin
 * interpretarlo, el log aplica histéresis y solo habla cuando alguien debe
 * actuar, y el resultado del job describe si el job funcionó y no lo que
 * encontró. Cada bloque de aquí sujeta una de esas tres decisiones, porque las
 * tres son contraintuitivas y las tres se pueden romper sin que nada más falle.
 */
@DisplayName("DatabaseAvailabilityProbe")
class DatabaseAvailabilityProbeTest {

    private static final Instant T0 = Instant.parse("2026-08-25T20:00:00Z");
    private static final int UMBRAL = 4;

    private MutableClock clock;
    private DataSource dataSource;
    private Connection connection;
    private MeterRegistry registry;
    private DatabaseAvailabilityProbe probe;

    private Logger canal;
    private RedactingAppender redactor;
    private ListAppender<ILoggingEvent> sink;
    private Level nivelPrevio;

    @BeforeEach
    void montar() throws SQLException {
        clock = new MutableClock();
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        when(connection.isValid(anyInt())).thenReturn(true);
        levantar();

        probe = new DatabaseAvailabilityProbe(dataSource,
                new ScheduledJobTelemetry(ObservationRegistry.NOOP), clock, 2, UMBRAL);
        registry = new SimpleMeterRegistry();
        probe.bindTo(registry);

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        sink = new ListAppender<>();
        sink.setContext(context);
        sink.start();

        // El sumidero va DETRAS del RedactingAppender, igual que en produccion
        // (REDACTED_JSON_CONSOLE envuelve a JSON_CONSOLE en logback-spring.xml). Sin
        // esta capa el test seria un falso verde: LogFieldPolicy es una allowlist, asi
        // que un addKeyValue con una clave sin declarar sale como '***' y las
        // aserciones sobre database.outage.seconds y database.failed.probes pasarian
        // igual leyendo el evento crudo. Cablearlo aqui hace que cada una de ellas
        // compruebe tambien que la clave esta en la allowlist.
        redactor = new RedactingAppender();
        redactor.setContext(context);
        redactor.setName("DATABASE_PROBE_REDACTION");
        redactor.addAppender(sink);
        redactor.start();

        canal = context.getLogger(DatabaseAvailabilityProbe.class);
        nivelPrevio = canal.getLevel();
        canal.setLevel(Level.INFO);
        canal.addAppender(redactor);
    }

    @AfterEach
    void desmontar() {
        canal.detachAppender(redactor);
        canal.setLevel(nivelPrevio);
        redactor.stop();
        sink.stop();
    }

    @Nested
    @DisplayName("la métrica es el hecho, sin histéresis")
    class LaMetricaEsElHecho {

        @Test
        @DisplayName("cae a cero en la PRIMERA sonda fallida, mucho antes del umbral")
        void cae_a_cero_en_la_primera_sonda_fallida() throws SQLException {
            caer();

            sondear(1);

            // Si la métrica esperase al umbral, el `for:` de la alerta se sumaría a la
            // histéresis del log y la detección tardaría el doble de lo que dice la
            // configuración. La histéresis vive en UN solo sitio: el log.
            assertThat(valor(DatabaseAvailabilityProbe.REACHABLE)).isZero();
            assertThat(sink.list).isEmpty();
        }

        @Test
        @DisplayName("la duración crece con el reloj y vuelve a cero al recuperarse")
        void la_duracion_crece_y_vuelve_a_cero() throws SQLException {
            caer();
            sondear(1);
            clock.avanzar(Duration.ofSeconds(90));

            assertThat(valor(DatabaseAvailabilityProbe.OUTAGE_DURATION)).isEqualTo(90d);

            levantar();
            sondear(1);

            assertThat(valor(DatabaseAvailabilityProbe.OUTAGE_DURATION)).isZero();
            assertThat(valor(DatabaseAvailabilityProbe.REACHABLE)).isEqualTo(1d);
        }

        @Test
        @DisplayName("con la base sana arranca y sigue en uno, sin escribir nada")
        void con_la_base_sana_no_escribe_nada() {
            assertThat(valor(DatabaseAvailabilityProbe.REACHABLE)).isEqualTo(1d);

            sondear(2);

            assertThat(valor(DatabaseAvailabilityProbe.REACHABLE)).isEqualTo(1d);
            assertThat(valor(DatabaseAvailabilityProbe.OUTAGE_DURATION)).isZero();
            assertThat(sink.list).isEmpty();
        }
    }

    @Nested
    @DisplayName("el log es el juicio: una línea por caída")
    class ElLogEsElJuicio {

        @Test
        @DisplayName("un fallo aislado no escribe: Hikari lo reintenta solo y no es noticia")
        void un_fallo_aislado_no_escribe() throws SQLException {
            caer();
            sondear(1);
            levantar();
            sondear(1);

            assertThat(sink.list).isEmpty();
        }

        @Test
        @DisplayName("al alcanzar el umbral emite UN ERROR y las sondas siguientes callan")
        void al_alcanzar_el_umbral_emite_un_solo_error() throws SQLException {
            caer();

            sondear(UMBRAL * 3);

            // El defecto que esta señal viene a arreglar era justamente el contrario:
            // veinte líneas de terceros describiendo un único hecho.
            assertThat(sink.list).hasSize(1);
            ILoggingEvent evento = sink.list.getFirst();
            assertThat(evento.getLevel()).isEqualTo(Level.ERROR);
            assertThat(pares(evento)).containsEntry("event", "database_unreachable")
                    .containsEntry("database.failed.probes", String.valueOf(UMBRAL));
        }

        @Test
        @DisplayName("el ERROR llega en la sonda del umbral, ni antes ni después")
        void el_error_llega_exactamente_en_el_umbral() throws SQLException {
            caer();

            sondear(UMBRAL - 1);
            assertThat(sink.list).isEmpty();

            sondear(1);
            assertThat(sink.list).hasSize(1);
        }

        @Test
        @DisplayName("la recuperación cierra el suceso en INFO con la duración escrita")
        void la_recuperacion_escribe_la_duracion() throws SQLException {
            caer();
            sondear(UMBRAL);
            clock.avanzar(Duration.ofSeconds(137));
            levantar();

            sondear(1);

            assertThat(sink.list).hasSize(2);
            ILoggingEvent recuperacion = sink.list.get(1);
            assertThat(recuperacion.getLevel()).isEqualTo(Level.INFO);
            assertThat(pares(recuperacion)).containsEntry("event", "database_reachable")
                    .containsEntry("database.outage.seconds", "137");
        }

        @Test
        @DisplayName("una segunda caída vuelve a escribir: el cierre rearma la señal")
        void una_segunda_caida_vuelve_a_escribir() throws SQLException {
            caer();
            sondear(UMBRAL);
            levantar();
            sondear(1);
            caer();
            sondear(UMBRAL);

            // Sin este rearme, la primera caída del proceso sería la única que se
            // reporta y todas las demás pasarían mudas.
            assertThat(sink.list).hasSize(3);
            assertThat(sink.list.get(2).getLevel()).isEqualTo(Level.ERROR);
        }

        @Test
        @DisplayName("una conexión que no supera isValid cuenta como fallo, no como éxito")
        void una_conexion_invalida_cuenta_como_fallo() throws SQLException {
            when(connection.isValid(anyInt())).thenReturn(false);

            sondear(UMBRAL);

            // El pool entrega una conexión muerta sin lanzar: si solo se mirase la
            // excepción, la sonda diría que todo va bien mientras nada funciona.
            assertThat(valor(DatabaseAvailabilityProbe.REACHABLE)).isZero();
            assertThat(sink.list).hasSize(1);
        }
    }

    @Nested
    @DisplayName("el resultado del job describe el job, no lo que encontró")
    class ElResultadoDescribeElJob {

        @Test
        @DisplayName("con la base caída el job informa SUCCESS y no toca la alerta de jobs")
        void con_la_base_caida_el_job_informa_success() throws SQLException {
            caer();
            ObservationRegistry observaciones = ObservationRegistry.create();
            ResultadoCapturado capturado = new ResultadoCapturado();
            observaciones.observationConfig().observationHandler(capturado);
            DatabaseAvailabilityProbe observada = new DatabaseAvailabilityProbe(dataSource,
                    new ScheduledJobTelemetry(observaciones), clock, 2, UMBRAL);

            observada.probe();

            // FAILURE aquí metería la señal en VetSoftwareScheduledJobFailing, que agrega
            // por job_name sobre job_outcome=~"failure|partial_failure|error", y una misma
            // caída dispararía dos alertas distintas.
            assertThat(capturado.outcome).isEqualTo("success");
            assertThat(capturado.jobName).isEqualTo("database.availability");
        }
    }

    @Nested
    @DisplayName("la sonda no deja rastro en el pool")
    class NoDejaRastroEnElPool {

        @Test
        @DisplayName("devuelve la conexión al pool en cada pasada")
        void devuelve_la_conexion_en_cada_pasada() throws SQLException {
            sondear(3);

            // Una sonda que filtrase una conexión por pasada agotaría un pool de diez en
            // menos de tres minutos y provocaría la caída que existe para detectar.
            verify(connection, times(3)).close();
        }
    }

    /** El bucle vive aquí y no en el cuerpo del test: ver «Testing conventions». */
    private void sondear(int veces) {
        java.util.stream.IntStream.range(0, veces).forEach(i -> probe.probe());
    }

    /**
     * {@code doThrow} y no {@code when(...).thenThrow(...)}: con el método ya
     * stubeado para lanzar, la forma {@code when} lo <em>invoca</em> para construir
     * el stub y revienta el propio montaje del test.
     */
    private void caer() throws SQLException {
        doThrow(new SQLException(
                "vetsoftware-hikari - Connection is not available, request timed out after 3000ms"))
                .when(dataSource).getConnection();
    }

    private void levantar() throws SQLException {
        doReturn(connection).when(dataSource).getConnection();
    }

    private double valor(String nombre) {
        return registry.get(nombre).gauge().value();
    }

    private static Map<String, String> pares(ILoggingEvent evento) {
        List<KeyValuePair> pares = evento.getKeyValuePairs();
        return pares == null
                ? Map.of()
                : pares.stream().collect(
                        Collectors.toMap(par -> par.key, par -> String.valueOf(par.value)));
    }

    /** Reloj que avanza a mano: la duración del corte se mide, no se espera. */
    private static final class MutableClock extends Clock {

        private Instant instante = T0;

        private void avanzar(Duration duracion) {
            instante = instante.plus(duracion);
        }

        @Override
        public Instant instant() {
            return instante;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    /**
     * Captura las dimensiones que {@link ScheduledJobTelemetry} pone en la
     * observación.
     */
    private static final class ResultadoCapturado
            implements
                ObservationHandler<Observation.Context> {

        private String outcome;
        private String jobName;

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }

        @Override
        public void onStop(Observation.Context context) {
            context.getLowCardinalityKeyValues()
                    .forEach(par -> capturar(par.getKey(), par.getValue()));
        }

        private void capturar(String clave, String valor) {
            if ("job.outcome".equals(clave)) {
                outcome = valor;
            } else if ("job.name".equals(clave)) {
                jobName = valor;
            }
        }
    }
}
