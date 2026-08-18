package com.vetsoftware.app.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TokenCleanupPropertiesTest {

    @Test
    void rejectsNonPositiveRetention() {
        TokenCleanupProperties properties = new TokenCleanupProperties();
        properties.setRetention(Duration.ZERO);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retention");
    }

    @Test
    @DisplayName("una retención negativa también se rechaza, no solo la nula")
    void rejectsNegativeRetention() {
        TokenCleanupProperties properties = new TokenCleanupProperties();
        properties.setRetention(Duration.ofDays(-1));

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retention");
    }

    @Test
    void rejectsUnboundedBatchConfiguration() {
        TokenCleanupProperties properties = new TokenCleanupProperties();
        properties.setMaxBatchesPerRun(101);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-batches-per-run");
    }

    @Test
    @DisplayName("un máximo de lotes por corrida menor que uno también se rechaza")
    void rejectsAMaxBatchesPerRunBelowOne() {
        TokenCleanupProperties properties = new TokenCleanupProperties();
        properties.setMaxBatchesPerRun(0);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("max-batches-per-run");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 10_001})
    @DisplayName("un tamaño de lote fuera de [1, 10000] se rechaza")
    void rejectsBatchSizeOutsideTheAllowedRange(int batchSize) {
        TokenCleanupProperties properties = new TokenCleanupProperties();
        properties.setBatchSize(batchSize);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("batch-size");
    }

    @Test
    @DisplayName("un umbral de alerta de crecimiento no positivo se rechaza")
    void rejectsANonPositiveGrowthWarningThreshold() {
        TokenCleanupProperties properties = new TokenCleanupProperties();
        properties.setGrowthWarningThreshold(0);

        assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("growth-warning-threshold");
    }

    @Nested
    @DisplayName("configuración válida")
    class ConfiguracionValida {

        @Test
        @DisplayName("los valores por defecto pasan la validación")
        void losValoresPorDefectoPasanLaValidacion() {
            TokenCleanupProperties properties = new TokenCleanupProperties();

            properties.validate();

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getRetention()).isEqualTo(Duration.ofDays(7));
            assertThat(properties.getBatchSize()).isEqualTo(1_000);
            assertThat(properties.getMaxBatchesPerRun()).isEqualTo(10);
            assertThat(properties.getGrowthWarningThreshold()).isEqualTo(50_000);
        }

        @Test
        @DisplayName("los setters se reflejan en los getters correspondientes")
        void losSettersSeReflejanEnLosGetters() {
            TokenCleanupProperties properties = new TokenCleanupProperties();

            properties.setEnabled(false);
            properties.setRetention(Duration.ofDays(30));
            properties.setBatchSize(500);
            properties.setMaxBatchesPerRun(5);
            properties.setGrowthWarningThreshold(1_000);
            properties.validate();

            assertThat(properties.isEnabled()).isFalse();
            assertThat(properties.getRetention()).isEqualTo(Duration.ofDays(30));
            assertThat(properties.getBatchSize()).isEqualTo(500);
            assertThat(properties.getMaxBatchesPerRun()).isEqualTo(5);
            assertThat(properties.getGrowthWarningThreshold()).isEqualTo(1_000);
        }
    }
}
