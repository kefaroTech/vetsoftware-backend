package com.vetsoftware.app.infrastructure.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import java.util.Objects;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/**
 * Enriquece la observación raíz que Spring crea para cada método {@code @Scheduled}.
 *
 * <p>Spring Boot configura automáticamente {@code tasks.scheduled.execution}. Esta clase no crea
 * una observación anidada cuando el job se ejecuta desde el scheduler; añade únicamente dimensiones
 * de negocio acotadas. El fallback conserva observabilidad cuando el método se invoca manualmente.
 */
@Component
public final class ScheduledJobTelemetry {

    static final String OBSERVATION_NAME = "tasks.scheduled.execution";
    static final String JOB_NAME_KEY = "job.name";
    static final String JOB_OUTCOME_KEY = "job.outcome";

    private final ObservationRegistry observationRegistry;

    public ScheduledJobTelemetry(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    public void observe(String jobName, Supplier<Outcome> action) {
        Objects.requireNonNull(jobName, "jobName es obligatorio");
        Objects.requireNonNull(action, "action es obligatoria");

        Observation current = observationRegistry.getCurrentObservation();
        if (current != null) {
            execute(current, jobName, action);
            return;
        }

        Observation root = Observation.createNotStarted(OBSERVATION_NAME, observationRegistry)
                .contextualName("task " + jobName);
        root.observe(() -> execute(root, jobName, action));
    }

    private static void execute(Observation observation, String jobName, Supplier<Outcome> action) {
        observation.lowCardinalityKeyValue(JOB_NAME_KEY, jobName);
        try {
            Outcome outcome = Objects.requireNonNull(action.get(), "El job debe informar un resultado");
            observation.lowCardinalityKeyValue(JOB_OUTCOME_KEY, outcome.value());
        } catch (RuntimeException | Error exception) {
            observation.lowCardinalityKeyValue(JOB_OUTCOME_KEY, Outcome.ERROR.value());
            throw exception;
        }
    }

    /**
     * Resultados deliberadamente acotados para no crear cardinalidad ilimitada en Prometheus.
     */
    public enum Outcome {
        NO_WORK("no_work"),
        SUCCESS("success"),
        PARTIAL_FAILURE("partial_failure"),
        FAILURE("failure"),
        ERROR("error");

        private final String value;

        Outcome(String value) {
            this.value = value;
        }

        public String value() {
            return value;
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
