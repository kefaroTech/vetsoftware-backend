package com.vetsoftware.app.infrastructure.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Señal propia de alcanzabilidad de la base de datos.
 *
 * <p>
 * <b>Por qué existe.</b> Con la base completamente inalcanzable, la aplicación
 * emitía WARN y nada más: doce de {@code org.hibernate.orm.jdbc.error}, ocho de
 * {@code com.zaxxer.hikari.pool.PoolBase} y ningún ERROR. Medido en dev el
 * 2026-08-25 sobre los 23 eventos de la ventana del corte. Los dos primeros
 * loggers son de terceros y fijan su severidad en su propia llamada: el
 * {@code <logger name="org.hibernate" level="WARN"/>} de
 * {@code logback-spring.xml} es un umbral, no una reescritura. Subirlos
 * exigiría un {@code TurboFilter} reescribiendo la severidad de una librería
 * ajena, y a partir de ahí ningún nivel de la aplicación volvería a significar
 * lo que dice. La alternativa correcta es que la severidad la decida un
 * componente nuestro, que es esta clase.
 *
 * <p>
 * <b>Reparto de responsabilidades.</b> Los tres escalones dicen cosas distintas
 * y se mantienen separados a propósito:
 *
 * <ul>
 * <li>la <b>métrica</b> es el hecho: {@code reachable} cae a 0 en la
 * <em>primera</em> sonda fallida, sin histéresis ni interpretación;
 * <li>el <b>log</b> es el juicio: aplica histéresis y solo habla cuando alguien
 * debe actuar;
 * <li>la <b>alerta</b> es la política: pone su propio {@code for:} sobre la
 * métrica, sin depender de cuántas líneas se escribieron.
 * </ul>
 *
 * <p>
 * <b>La severidad sigue el criterio del repositorio: quién debe actuar.</b> Es
 * el mismo con el que {@code 6e48322e} bajó los 4xx del manejador global a
 * INFO. Una sonda fallida suelta es un parpadeo que Hikari reintenta solo y no
 * es noticia; una racha sostenida sí lo es. Y se emite <b>una sola línea por
 * caída</b>: la tormenta de veinte WARN describía un único hecho y hacía
 * ilegible el suceso.
 *
 * <p>
 * <b>La sonda pasa por el pool a propósito</b>, no por una conexión aparte. La
 * pregunta que importa no es «¿responde el servidor?» sino «¿puede la
 * aplicación conseguir una conexión?». Son distintas: el 2026-08-25 por la
 * tarde hubo una tarea viva y {@code HEALTHY} para ECS que no lograba abrir ni
 * una sola conexión porque su contraseña había quedado obsoleta tras rotarse el
 * secreto. El servidor respondía perfectamente. Pasar por el pool también hace
 * que {@code hikaricp_connections_timeout_total} siga contando durante una
 * caída aunque no haya tráfico de usuario, de modo que la alerta que se apoya
 * en ese contador deja de depender de que alguien esté usando la aplicación.
 *
 * <p>
 * <b>El job informa SUCCESS aunque la base esté caída</b>, y esto es
 * contraintuitivo. El resultado de un job describe si el <em>job</em> funcionó,
 * no lo que encontró: una sonda que mide correctamente «la base no responde»
 * hizo su trabajo. Informar FAILURE metería esta señal en
 * {@code VetSoftwareScheduledJobFailing}, que agrega por {@code job_name} sobre
 * {@code job_outcome=~"failure|partial_failure|error"}, y una misma caída
 * dispararía dos alertas distintas. Solo un fallo de la propia maquinaria
 * escapa como excepción y {@link ScheduledJobTelemetry} lo marca ERROR.
 *
 * <p>
 * <b>Limitación conocida.</b> El planificador tiene un solo hilo
 * ({@code SchedulingConfig} no configura
 * {@code spring.task.scheduling.pool.size}), así que durante una caída esta
 * sonda lo ocupa hasta {@code spring.datasource.hikari.connection-timeout} (3
 * s) por pasada. Si otro job lo retiene, la sonda no corre y sus medidores
 * dejan de actualizarse; no se corrompen, porque la duración se deriva de un
 * instante y no de un acumulador.
 *
 * <p>
 * <b>Lo que esta señal NO puede cubrir.</b> La emite el propio proceso: si el
 * proceso muere, la serie desaparece y las reglas con {@code noDataState: OK}
 * la leen como salud. Eso se resuelve fuera —el heartbeat
 * {@code VetSoftwareBackendTelemetryAbsent} en prod, las alarmas de ECS del
 * lado de AWS— y no hay instrumentación dentro del proceso que lo arregle.
 *
 * @see ScheduledJobTelemetry
 */
@Component
public class DatabaseAvailabilityProbe implements MeterBinder {

    /** 1 si la última sonda consiguió una conexión válida, 0 si no. */
    public static final String REACHABLE = "vetsoftware.database.reachable";

    /** Segundos que lleva la racha de sondas fallidas en curso. */
    public static final String OUTAGE_DURATION = "vetsoftware.database.outage.duration";

    private static final Logger log = LoggerFactory.getLogger(DatabaseAvailabilityProbe.class);

    private static final String JOB_NAME = "database.availability";

    private final DataSource dataSource;
    private final Clock clock;
    private final ScheduledJobTelemetry telemetry;
    private final int validationTimeoutSeconds;
    private final int failureThreshold;

    /**
     * Arranca en 1, y no en 0, porque si el proceso está vivo la base respondía:
     * Liquibase corre en el arranque y {@code ddl-auto: validate} exige el esquema.
     * Un 0 inicial sería una afirmación falsa durante el primer intervalo.
     */
    private final AtomicLong reachable = new AtomicLong(1);

    /** Instante de la primera sonda fallida de la racha; 0 si no hay racha. */
    private final AtomicLong outageStartedEpochSecond = new AtomicLong();

    /** Solo lo toca el hilo del planificador; los medidores no lo leen. */
    private int consecutiveFailures;

    /** Cierra la puerta al segundo ERROR de la misma caída. */
    private boolean outageReported;

    @Autowired
    public DatabaseAvailabilityProbe(DataSource dataSource, ScheduledJobTelemetry telemetry,
            @Value("${vetsoftware.observability.database-availability.validation-timeout-seconds:2}") int validationTimeoutSeconds,
            @Value("${vetsoftware.observability.database-availability.failure-threshold:4}") int failureThreshold) {
        this(dataSource, telemetry, Clock.systemUTC(), validationTimeoutSeconds, failureThreshold);
    }

    DatabaseAvailabilityProbe(DataSource dataSource, ScheduledJobTelemetry telemetry, Clock clock,
            int validationTimeoutSeconds, int failureThreshold) {
        this.dataSource = dataSource;
        this.telemetry = telemetry;
        this.clock = clock;
        this.validationTimeoutSeconds = validationTimeoutSeconds;
        this.failureThreshold = failureThreshold;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        // Sin etiquetas: una sola serie. Un identificador de instancia aquí no
        // respondería ninguna pregunta que el recurso OTel no responda ya.
        Gauge.builder(REACHABLE, reachable, AtomicLong::doubleValue)
                .description("1 si la ultima sonda consiguio una conexion valida de la base de"
                        + " datos, 0 si no")
                .register(registry);
        Gauge.builder(OUTAGE_DURATION, this, DatabaseAvailabilityProbe::outageDurationSeconds)
                .baseUnit("seconds")
                .description("Segundos desde la primera sonda fallida de la racha en curso; 0 si"
                        + " la base responde")
                .register(registry);
    }

    @Scheduled(initialDelayString = "${vetsoftware.observability.database-availability.initial-delay-ms:15000}", fixedDelayString = "${vetsoftware.observability.database-availability.probe-interval-ms:15000}")
    public void probe() {
        telemetry.observe(JOB_NAME, this::runProbe);
    }

    private ScheduledJobTelemetry.Outcome runProbe() {
        Throwable failure = null;
        boolean reachableNow;
        try (Connection connection = dataSource.getConnection()) {
            reachableNow = connection.isValid(validationTimeoutSeconds);
        } catch (SQLException | RuntimeException exception) {
            reachableNow = false;
            failure = exception;
        }

        if (reachableNow) {
            recordSuccess();
        } else {
            recordFailure(failure);
        }
        // SUCCESS incluso con la base caída: ver el javadoc de la clase.
        return ScheduledJobTelemetry.Outcome.SUCCESS;
    }

    private void recordSuccess() {
        reachable.set(1);
        consecutiveFailures = 0;
        long startedAt = outageStartedEpochSecond.getAndSet(0);
        if (!outageReported) {
            return;
        }
        outageReported = false;
        // INFO y no WARN: el suceso ya se contó al empezar, y esta línea existe para
        // cerrarlo y dejar la duración escrita. Nadie tiene que actuar sobre ella.
        log.atInfo().addKeyValue("event", "database_reachable")
                .addKeyValue("database.outage.seconds", secondsSince(startedAt))
                .log("la base de datos volvio a responder");
    }

    private void recordFailure(Throwable failure) {
        reachable.set(0);
        consecutiveFailures++;
        outageStartedEpochSecond.compareAndSet(0, clock.instant().getEpochSecond());
        if (outageReported || consecutiveFailures < failureThreshold) {
            return;
        }
        outageReported = true;
        log.atError().addKeyValue("event", "database_unreachable")
                .addKeyValue("database.failed.probes", consecutiveFailures).setCause(failure)
                .log("la base de datos no responde: {} sondas consecutivas sin conexion",
                        consecutiveFailures);
    }

    private double outageDurationSeconds() {
        return secondsSince(outageStartedEpochSecond.get());
    }

    private long secondsSince(long epochSecond) {
        if (epochSecond == 0) {
            return 0;
        }
        return ChronoUnit.SECONDS.between(Instant.ofEpochSecond(epochSecond), clock.instant());
    }
}
