package com.vetsoftware.app.infrastructure.observability;

import com.vetsoftware.app.infrastructure.logging.MdcKeys;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Enriquece la observación raíz que Spring crea para cada método
 * {@code @Scheduled}.
 *
 * <p>
 * Spring Boot configura automáticamente {@code tasks.scheduled.execution}. Esta
 * clase no crea una observación anidada cuando el job se ejecuta desde el
 * scheduler; añade únicamente dimensiones de negocio acotadas. El fallback
 * conserva observabilidad cuando el método se invoca manualmente.
 *
 * <p>
 * <b>También pone el actor en el MDC (issue #607).</b> Un barrido no cruza el
 * borde HTTP, así que ni {@code AuthFilter} ni
 * {@code RequestLoggingContextFilter} llegan a poblar nada: todo lo que un job
 * escribiera —incluidos los eventos de {@code AuditLogger}— salía sin actor,
 * sin origen y sin forma de distinguirlo de una operación de una persona. Aquí
 * se declara {@code actor.type=SYSTEM} y {@code job.name}, que es el «desde
 * dónde» que piden NIST SP 800-53 AU-3 y PCI DSS v4.0 req. 10.2 cuando el
 * origen es un proceso. Los valores previos se restauran en el {@code finally}:
 * el hilo del scheduler se reutiliza, y dejar residuo en el MDC atribuiría el
 * siguiente job al anterior.
 *
 * <p>
 * <b>Y sella el heartbeat.</b> {@link ScheduledJobHeartbeat} publica el
 * instante del último final correcto, que es lo único que permite alertar sobre
 * un barrido que <b>no corrió</b> —cosa que ningún contador de fallos puede
 * detectar—. Se sella aquí y no en cada job para que el instante que publica la
 * métrica sea el mismo que cierra la observación; dos emisores para el mismo
 * hecho es exactamente cómo se llega a que la traza diga una cosa y la métrica
 * otra.
 */
@Component
public final class ScheduledJobTelemetry {

    static final String OBSERVATION_NAME = "tasks.scheduled.execution";
    static final String JOB_NAME_KEY = "job.name";
    static final String JOB_OUTCOME_KEY = "job.outcome";

    /** Valor de {@code actor.type} para todo lo que hace un barrido. */
    public static final String SYSTEM_ACTOR = "SYSTEM";

    private static final Pattern JOB_NAME_PATTERN = Pattern
            .compile("^[a-z][a-z0-9]*(?:\\.[a-z][a-z0-9]*)+$");

    private final ObservationRegistry observationRegistry;
    private final ScheduledJobHeartbeat heartbeat;

    /**
     * <b>{@code @Autowired} es obligatorio aquí, no decorativo.</b> Esta clase
     * declara DOS constructores públicos, y con más de uno Spring ya no puede
     * elegir por descarte: cae al constructor sin argumentos, que no existe, y el
     * contexto entero muere con {@code NoSuchMethodException: <init>()}. No falla
     * solo este bean — {@code UsageReconciliationJob} depende de él y arrastra a
     * toda la aplicación, así que el síntoma es que NADA arranca, incluidos los
     * tests de integración.
     *
     * <p>
     * La anotación marca cuál de los dos es el punto de inyección, que es justo lo
     * que el javadoc del constructor de un argumento ya daba por sentado.
     */
    @Autowired
    public ScheduledJobTelemetry(ObservationRegistry observationRegistry,
            ScheduledJobHeartbeat heartbeat) {
        this.observationRegistry = observationRegistry;
        this.heartbeat = heartbeat;
    }

    /**
     * Sin heartbeat. Reservado para usos donde no hay registro de métricas —el
     * {@code noopTelemetry()} de {@code BusinessGaugeMetrics} y las pruebas
     * unitarias de esta clase—: un barrido de producción entra siempre por el
     * constructor de dos argumentos, que es el que Spring inyecta.
     */
    public ScheduledJobTelemetry(ObservationRegistry observationRegistry) {
        this(observationRegistry, null);
    }

    /**
     * Camino de los barridos de calendario. Exige una constante del catálogo en vez
     * de una cadena porque el nombre es la etiqueta de la que cuelgan las dos
     * alertas: escrito como literal, un typo creaba una serie nueva y dejaba la
     * alerta vigilando un nombre que ya no emitía nadie, verde para siempre.
     */
    public void observe(ScheduledJobCatalog job, Supplier<Outcome> action) {
        Objects.requireNonNull(job, "job es obligatorio");
        observe(job.jobName(), action);
    }

    /**
     * Camino de las sondas de muestreo continuo ({@code database.availability},
     * {@code business.metrics.snapshot}), que no están en el catálogo porque no se
     * rigen por calendario. No sellan heartbeat: su retraso ya lo vigilan sus
     * propias señales.
     */
    public void observe(String jobName, Supplier<Outcome> action) {
        validateJobName(jobName);
        Objects.requireNonNull(action, "action es obligatoria");

        String previousActor = MDC.get(MdcKeys.ACTOR_TYPE);
        String previousJob = MDC.get(MdcKeys.JOB_NAME);
        MDC.put(MdcKeys.ACTOR_TYPE, SYSTEM_ACTOR);
        MDC.put(MdcKeys.JOB_NAME, jobName);
        try {
            Observation current = observationRegistry.getCurrentObservation();
            if (current != null) {
                execute(current, jobName, action);
                return;
            }

            Observation root = Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)
                    .contextualName("run " + jobName.replace('.', ' '));
            root.observe(() -> execute(root, jobName, action));
        } finally {
            restore(MdcKeys.ACTOR_TYPE, previousActor);
            restore(MdcKeys.JOB_NAME, previousJob);
        }
    }

    private static void restore(String key, String previous) {
        if (previous == null) {
            MDC.remove(key);
            return;
        }
        MDC.put(key, previous);
    }

    private static void validateJobName(String jobName) {
        Objects.requireNonNull(jobName, "jobName es obligatorio");
        if (!JOB_NAME_PATTERN.matcher(jobName).matches()) {
            throw new IllegalArgumentException(
                    "jobName debe usar lowercase.dot.notation: " + jobName);
        }
    }

    private void execute(Observation observation, String jobName, Supplier<Outcome> action) {
        observation.lowCardinalityKeyValue(JOB_NAME_KEY, jobName);
        try {
            Outcome outcome = Objects.requireNonNull(action.get(),
                    "El job debe informar un resultado");
            observation.lowCardinalityKeyValue(JOB_OUTCOME_KEY, outcome.value());
            if (outcome.sealsHeartbeat()) {
                sealHeartbeat(jobName);
            }
        } catch (RuntimeException | Error exception) {
            observation.lowCardinalityKeyValue(JOB_OUTCOME_KEY, Outcome.ERROR.value());
            throw exception;
        }
    }

    private void sealHeartbeat(String jobName) {
        if (heartbeat == null) {
            return;
        }
        ScheduledJobCatalog.byJobName(jobName).ifPresent(heartbeat::recordSuccess);
    }

    /**
     * Resultados deliberadamente acotados para no crear cardinalidad ilimitada en
     * Prometheus.
     */
    public enum Outcome {
        NO_WORK("no_work"), SUCCESS("success"), PARTIAL_FAILURE("partial_failure"), FAILURE(
                "failure"), ERROR("error");

        private final String value;

        Outcome(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        /**
         * {@code true} si el barrido llegó al final por su propio pie.
         *
         * <p>
         * {@code PARTIAL_FAILURE} sella a propósito: el job corrió, y quien vigila que
         * corriera no es quien vigila que no fallara. Confundir las dos preguntas es lo
         * que hace que un job que falla todas las noches parezca también un job que no
         * se ejecuta, y que arreglar lo segundo apague la alerta de lo primero.
         * {@code FAILURE} y {@code ERROR} no sellan: ahí el trabajo del ciclo no se
         * hizo.
         */
        public boolean sealsHeartbeat() {
            return this == NO_WORK || this == SUCCESS || this == PARTIAL_FAILURE;
        }

        public static Outcome from(int attempted, int failures) {
            if (attempted < 0 || failures < 0 || failures > attempted) {
                throw new IllegalArgumentException("Conteos inválidos del job");
            }
            if (attempted == 0) {
                return NO_WORK;
            }
            if (failures == 0) {
                return SUCCESS;
            }
            return failures == attempted ? FAILURE : PARTIAL_FAILURE;
        }
    }
}
