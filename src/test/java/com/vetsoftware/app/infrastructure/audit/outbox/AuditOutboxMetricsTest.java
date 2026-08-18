package com.vetsoftware.app.infrastructure.audit.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditOutboxMetrics")
class AuditOutboxMetricsTest {

    @Mock
    private AuditOutboxRepository repository;

    private SimpleMeterRegistry registry;
    private AuditOutboxMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AuditOutboxMetrics(registry, repository);
    }

    @Nested
    @DisplayName("audit.outbox.pending")
    class Pending {

        @Test
        @DisplayName("delega en el conteo de pendientes del repositorio")
        void delega_en_el_conteo_de_pendientes() {
            when(repository.pendingCount()).thenReturn(9L);

            assertThat(registry.get("audit.outbox.pending").gauge().value()).isEqualTo(9.0);
        }

        @Test
        @DisplayName("reporta NaN si el repositorio falla")
        void reporta_nan_si_el_repositorio_falla() {
            when(repository.pendingCount()).thenThrow(new RuntimeException("bd caida"));

            assertThat(registry.get("audit.outbox.pending").gauge().value()).isNaN();
        }
    }

    @Nested
    @DisplayName("audit.outbox.failed")
    class Failed {

        @Test
        @DisplayName("delega en el conteo de fallidos del repositorio")
        void delega_en_el_conteo_de_fallidos() {
            when(repository.failedCount()).thenReturn(3L);

            assertThat(registry.get("audit.outbox.failed").gauge().value()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("reporta NaN si el repositorio falla")
        void reporta_nan_si_el_repositorio_falla() {
            when(repository.failedCount()).thenThrow(new RuntimeException("bd caida"));

            assertThat(registry.get("audit.outbox.failed").gauge().value()).isNaN();
        }
    }

    @Nested
    @DisplayName("audit.outbox.oldest.age")
    class OldestAge {

        @Test
        @DisplayName("delega en la edad del pendiente mas antiguo del repositorio")
        void delega_en_la_edad_del_pendiente_mas_antiguo() {
            when(repository.oldestPendingAgeSeconds()).thenReturn(88.0);

            assertThat(registry.get("audit.outbox.oldest.age").gauge().value()).isEqualTo(88.0);
        }

        @Test
        @DisplayName("reporta NaN si el repositorio falla")
        void reporta_nan_si_el_repositorio_falla() {
            when(repository.oldestPendingAgeSeconds()).thenThrow(new RuntimeException("bd caida"));

            assertThat(registry.get("audit.outbox.oldest.age").gauge().value()).isNaN();
        }
    }

    @Nested
    @DisplayName("contadores")
    class Contadores {

        @Test
        @DisplayName("published incrementa el contador de eventos aceptados por Firehose")
        void published_incrementa_el_contador() {
            metrics.published(5);

            assertThat(registry.get("audit.outbox.published").counter().count()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("failed incrementa el contador de intentos de publicacion fallidos")
        void failed_incrementa_el_contador() {
            metrics.failed(2);

            assertThat(registry.get("audit.outbox.publish.failures").counter().count())
                    .isEqualTo(2.0);
        }
    }
}
