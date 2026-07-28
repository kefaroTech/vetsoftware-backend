package com.vetsoftware.app.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

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
        assertThat(context.getContextualName()).isEqualTo("task dian.test");
        assertThat(context.getParentObservation()).isNull();
        assertThat(lowCardinalityValue(context, ScheduledJobTelemetry.JOB_NAME_KEY)).isEqualTo("dian.test");
        assertThat(lowCardinalityValue(context, ScheduledJobTelemetry.JOB_OUTCOME_KEY)).isEqualTo("success");
        assertThat(context.getError()).isNull();
    }

    @Test
    void enrichesSpringObservationWithoutCreatingNestedObservation() {
        CapturingHandler handler = new CapturingHandler();
        ObservationRegistry registry = registryWith(handler);
        ScheduledJobTelemetry telemetry = new ScheduledJobTelemetry(registry);
        Observation scheduledRoot = Observation.start(ScheduledJobTelemetry.OBSERVATION_NAME, registry);

        try (Observation.Scope ignored = scheduledRoot.openScope()) {
            telemetry.observe("dian.test", () -> Outcome.PARTIAL_FAILURE);
            assertThat(registry.getCurrentObservation()).isSameAs(scheduledRoot);
        } finally {
            scheduledRoot.stop();
        }

        assertThat(handler.stopped).hasSize(1);
        Observation.Context context = handler.stopped.getFirst();
        assertThat(context.getParentObservation()).isNull();
        assertThat(lowCardinalityValue(context, ScheduledJobTelemetry.JOB_NAME_KEY)).isEqualTo("dian.test");
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
        assertThat(lowCardinalityValue(context, ScheduledJobTelemetry.JOB_OUTCOME_KEY)).isEqualTo("error");
        assertThat(registry.getCurrentObservation()).isNull();
    }

    @Test
    void mapsBoundedBusinessOutcomes() {
        assertThat(Outcome.from(0, 0)).isEqualTo(Outcome.NO_WORK);
        assertThat(Outcome.from(2, 0)).isEqualTo(Outcome.SUCCESS);
        assertThat(Outcome.from(2, 1)).isEqualTo(Outcome.PARTIAL_FAILURE);
        assertThat(Outcome.from(2, 2)).isEqualTo(Outcome.FAILURE);
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
