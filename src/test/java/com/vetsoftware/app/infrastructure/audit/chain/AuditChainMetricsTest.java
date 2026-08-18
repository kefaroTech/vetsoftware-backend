package com.vetsoftware.app.infrastructure.audit.chain;

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
@DisplayName("AuditChainMetrics")
class AuditChainMetricsTest {

    @Mock
    private AuditChainRepository repository;

    private SimpleMeterRegistry registry;
    private AuditChainMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new AuditChainMetrics(registry, repository);
    }

    @Test
    @DisplayName("arranca con audit.chain.broken en -1 porque todavia no se verifico nada")
    void arranca_sin_verificar() {
        assertThat(registry.get("audit.chain.broken").gauge().value()).isEqualTo(-1.0);
        assertThat(registry.get("audit.chain.verified.sequence").gauge().value()).isZero();
        assertThat(registry.get("audit.chain.failure.sequence").gauge().value()).isZero();
        assertThat(registry.get("audit.chain.checkpoint.sequence").gauge().value()).isZero();
    }

    @Nested
    @DisplayName("audit.chain.length")
    class ChainLength {

        @Test
        @DisplayName("delega en la longitud de la cadena del repositorio")
        void delega_en_la_longitud_de_la_cadena() {
            when(repository.head()).thenReturn(new AuditChainRepository.Head(120L, "hash", 100L));

            assertThat(registry.get("audit.chain.length").gauge().value()).isEqualTo(120.0);
        }

        @Test
        @DisplayName("reporta NaN si el repositorio falla al consultar la cabeza")
        void reporta_nan_si_el_repositorio_falla() {
            when(repository.head()).thenThrow(new RuntimeException("bd caida"));

            assertThat(registry.get("audit.chain.length").gauge().value()).isNaN();
        }
    }

    @Nested
    @DisplayName("audit.chain.unsequenced")
    class Unsequenced {

        @Test
        @DisplayName("delega en el conteo de eventos sin posicion del repositorio")
        void delega_en_el_conteo_de_eventos_sin_posicion() {
            when(repository.unsequencedCount()).thenReturn(7L);

            assertThat(registry.get("audit.chain.unsequenced").gauge().value()).isEqualTo(7.0);
        }

        @Test
        @DisplayName("reporta NaN si el repositorio falla al contar")
        void reporta_nan_si_el_repositorio_falla_al_contar() {
            when(repository.unsequencedCount()).thenThrow(new RuntimeException("bd caida"));

            assertThat(registry.get("audit.chain.unsequenced").gauge().value()).isNaN();
        }
    }

    @Nested
    @DisplayName("verified")
    class Verified {

        @Test
        @DisplayName("una verificacion intacta pone broken en cero")
        void una_verificacion_intacta_pone_broken_en_cero() {
            metrics.verified(new AuditChainVerifier.Result(true, 40, 40L, "hash-40", 0, null));

            assertThat(registry.get("audit.chain.broken").gauge().value()).isZero();
            assertThat(registry.get("audit.chain.verified.sequence").gauge().value())
                    .isEqualTo(40.0);
            assertThat(registry.get("audit.chain.failure.sequence").gauge().value()).isZero();
        }

        @Test
        @DisplayName("una verificacion rota pone broken en uno y registra la posicion del fallo")
        void una_verificacion_rota_pone_broken_en_uno() {
            metrics.verified(new AuditChainVerifier.Result(false, 12, 12L, "hash-12", 13L,
                    "hueco en la cadena"));

            assertThat(registry.get("audit.chain.broken").gauge().value()).isEqualTo(1.0);
            assertThat(registry.get("audit.chain.verified.sequence").gauge().value())
                    .isEqualTo(12.0);
            assertThat(registry.get("audit.chain.failure.sequence").gauge().value())
                    .isEqualTo(13.0);
        }
    }

    @Nested
    @DisplayName("checkpointed")
    class Checkpointed {

        @Test
        @DisplayName("actualiza la ultima secuencia anclada")
        void actualiza_la_ultima_secuencia_anclada() {
            metrics.checkpointed(77L);

            assertThat(registry.get("audit.chain.checkpoint.sequence").gauge().value())
                    .isEqualTo(77.0);
        }
    }
}
