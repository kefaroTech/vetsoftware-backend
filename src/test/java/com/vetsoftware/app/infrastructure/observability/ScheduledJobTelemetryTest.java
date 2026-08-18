package com.vetsoftware.app.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class ScheduledJobTelemetryTest {

    @Test
    void createsRootObservationWhenThereIsNoSchedulerContext() {
        CapturingHandler handler = new CapturingHandler();
        ObservationRegistry registry = registryWith(handler);
        ScheduledJobTelemetry telemetry = new ScheduledJobTelemetry(registry);

        telemetry.observe("dian.test", () -> Outcome.SUCCESS);

        assertThat(handler.stopped).hasSize(1);
        Observation.Context context = handler.stopped.getFirst();
        assertThat(context.getName()).isEqualTo(ScheduledJobTelemetry.OBSERVATION_NAME);
        assertThat(context.getContextualName()).isEqualTo("run dian test");
        assertThat(context.getParentObservation()).isNull();
        assertThat(lowCardinalityValue(context, ScheduledJobTelemetry.JOB_NAME_KEY))
                .isEqualTo("dian.test");
        assertThat(lowCardinalityValue(context, ScheduledJobTelemetry.JOB_OUTCOME_KEY))
                .isEqualTo("success");
        assertThat(context.getError()).isNull();
    }

    @Test
    void enrichesSpringObservationWithoutCreatingNestedObservation() {
        CapturingHandler handler = new CapturingHandler();
        ObservationRegistry registry = registryWith(handler);
        ScheduledJobTelemetry telemetry = new ScheduledJobTelemetry(registry);
        Observation scheduledRoot = Observation.start(ScheduledJobTelemetry.OBSERVATION_NAME,
                registry);

        try (Observation.Scope ignored = scheduledRoot.openScope()) {
            telemetry.observe("dian.test", () -> Outcome.PARTIAL_FAILURE);
            assertThat(registry.getCurrentObservation()).isSameAs(scheduledRoot);
        } finally {
            scheduledRoot.stop();
        }

        assertThat(handler.stopped).hasSize(1);
        Observation.Context context = handler.stopped.getFirst();
        assertThat(context.getParentObservation()).isNull();
        assertThat(lowCardinalityValue(context, ScheduledJobTelemetry.JOB_NAME_KEY))
                .isEqualTo("dian.test");
        assertThat(lowCardinalityValue(context, ScheduledJobTelemetry.JOB_OUTCOME_KEY))
                .isEqualTo("partial_failure");
    }

    @Test
    void recordsUncontrolledErrorAndAlwaysStopsFallbackObservation() {
        CapturingHandler handler = new CapturingHandler();
        ObservationRegistry registry = registryWith(handler);
        ScheduledJobTelemetry telemetry = new ScheduledJobTelemetry(registry);
        IllegalStateException failure = new IllegalStateException("database unavailable");

        assertThatThrownBy(() -> telemetry.observe("dian.test", () -> {
            throw failure;
        })).isSameAs(failure);

        assertThat(handler.stopped).hasSize(1);
        Observation.Context context = handler.stopped.getFirst();
        assertThat(context.getError()).isSameAs(failure);
        assertThat(lowCardinalityValue(context, ScheduledJobTelemetry.JOB_OUTCOME_KEY))
                .isEqualTo("error");
        assertThat(registry.getCurrentObservation()).isNull();
    }

    @Test
    void mapsBoundedBusinessOutcomes() {
        assertThat(Outcome.from(0, 0)).isEqualTo(Outcome.NO_WORK);
        assertThat(Outcome.from(2, 0)).isEqualTo(Outcome.SUCCESS);
        assertThat(Outcome.from(2, 1)).isEqualTo(Outcome.PARTIAL_FAILURE);
        assertThat(Outcome.from(2, 2)).isEqualTo(Outcome.FAILURE);
    }

    @Test
    void rejectsJobNamesOutsideLowercaseDotNotation() {
        ScheduledJobTelemetry telemetry = new ScheduledJobTelemetry(ObservationRegistry.create());

        assertThatThrownBy(() -> telemetry.observe("audit_outbox.publish", () -> Outcome.SUCCESS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase.dot.notation");
        assertThatThrownBy(() -> telemetry.observe("auditOutbox.publish", () -> Outcome.SUCCESS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lowercase.dot.notation");
    }

    @ParameterizedTest
    @MethodSource("invalidOutcomeCounts")
    @DisplayName("conteos inválidos de un job (negativos o fallos por encima de lo intentado) se rechazan")
    void rejectsInvalidOutcomeCounts(int attempted, int failures) {
        assertThatThrownBy(() -> Outcome.from(attempted, failures))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Conteos inválidos");
    }

    private static Stream<Arguments> invalidOutcomeCounts() {
        return Stream.of(Arguments.of(-1, 0), Arguments.of(0, -1), Arguments.of(2, 3));
    }

    @ParameterizedTest
    @EnumSource(Outcome.class)
    @DisplayName("cada resultado expone un valor de baja cardinalidad estable para Prometheus")
    void everyOutcomeExposesItsBoundedPrometheusValue(Outcome outcome) {
        assertThat(outcome.value()).isEqualTo(outcome.name().toLowerCase(java.util.Locale.ROOT));
    }

    private static ObservationRegistry registryWith(CapturingHandler handler) {
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(handler);
        return registry;
    }

    private static String lowCardinalityValue(Observation.Context context, String key) {
        return context.getLowCardinalityKeyValue(key).getValue();
    }

    private static final class CapturingHandler implements ObservationHandler<Observation.Context> {
        private final List<Observation.Context> stopped = new ArrayList<>();

        @Override
        public void onStop(Observation.Context context) {
            stopped.add(context);
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }
    }
}
