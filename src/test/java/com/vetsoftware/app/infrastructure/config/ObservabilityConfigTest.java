package com.vetsoftware.app.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import org.junit.jupiter.api.Test;

class ObservabilityConfigTest {

    @Test
    void publishesTheObservedAspectOverTheApplicationRegistry() {
        ObservationRegistry registry = ObservationRegistry.create();

        ObservedAspect aspect = new ObservabilityConfig().observedAspect(registry);

        assertThat(aspect).isNotNull();
    }
}
