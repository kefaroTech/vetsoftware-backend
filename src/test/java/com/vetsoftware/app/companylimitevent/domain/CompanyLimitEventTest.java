package com.vetsoftware.app.companylimitevent.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyLimitEvent — la fila que documenta el portazo")
class CompanyLimitEventTest {

    private static final Long ANA = 42L;
    private static final Long EJE_ANIMAL = 1L;
    private static final LocalDateTime EN_MARZO = LocalDateTime.of(2026, 3, 14, 10, 30);

    @Nested
    @DisplayName("R-LIMIT-32 · los tres números van copiados, no referenciados")
    class NumerosCopiados {

        @Test
        @DisplayName("subir el techo de 100 a 300 no cambia lo que dice un evento de marzo")
        void subir_el_techo_de_100_a_300_no_cambia_lo_que_dice_un_evento_de_marzo() {
            CompanyLimitEvent portazo = CompanyLimitEvent.record(ANA, EJE_ANIMAL,
                    LimitEventType.LIMIT_BLOCKED, 100, 100, 1, LimitSource.CATALOG_DEFAULT, null,
                    EventActor.employee(9L), null, null, EN_MARZO);

            assertThat(portazo.getLimitQuantity()).isEqualTo(100);
            assertThat(portazo.getUsedQuantity()).isEqualTo(100);
            assertThat(portazo.getRequestedDelta()).isEqualTo(1);
            assertThat(Arrays.stream(CompanyLimitEvent.class.getMethods()).map(Method::getName))
                    .noneMatch(nombre -> nombre.startsWith("set"));
        }

        @Test
        @DisplayName("un techo negociado tiene que nombrar la excepción de la que salió")
        void un_techo_negociado_tiene_que_nombrar_su_excepcion() {
            assertThatThrownBy(() -> CompanyLimitEvent.record(ANA, EJE_ANIMAL,
                    LimitEventType.LIMIT_BLOCKED, 300, 300, 1, LimitSource.COMPANY_OVERRIDE, null,
                    EventActor.employee(9L), null, null, EN_MARZO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must name the override");
        }

        @Test
        @DisplayName("un techo que no es negociado no puede nombrar una excepción")
        void un_techo_no_negociado_no_nombra_excepcion() {
            assertThatThrownBy(() -> CompanyLimitEvent.record(ANA, EJE_ANIMAL,
                    LimitEventType.LIMIT_BLOCKED, 100, 100, 1, LimitSource.CATALOG_DEFAULT, 5L,
                    EventActor.employee(9L), null, null, EN_MARZO))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("R-LIMIT-19 · la corrección lleva motivo obligatorio")
    class CorreccionConMotivo {

        @Test
        @DisplayName("corregir 500 mascotas duplicadas de una migración escribe USAGE_ADJUSTED con"
                + " su motivo y su firma")
        void corregir_500_mascotas_duplicadas_escribe_USAGE_ADJUSTED_con_motivo() {
            CompanyLimitEvent correccion = CompanyLimitEvent.record(ANA, EJE_ANIMAL,
                    LimitEventType.USAGE_ADJUSTED, 100, 600, -500, LimitSource.NONE, null,
                    EventActor.systemUser(3L), "MIGRATION",
                    "Migración duplicada del 14/03, ticket SOP-118", EN_MARZO);

            assertThat(correccion.getEventType()).isEqualTo(LimitEventType.USAGE_ADJUSTED);
            assertThat(correccion.getRequestedDelta()).isEqualTo(-500);
            assertThat(correccion.getActor().systemUserId()).isEqualTo(3L);
            assertThat(correccion.getReason()).contains("SOP-118");
        }

        @Test
        @DisplayName("una corrección sin motivo escrito se rechaza")
        void una_correccion_sin_motivo_escrito_se_rechaza() {
            assertThatThrownBy(() -> CompanyLimitEvent.record(ANA, EJE_ANIMAL,
                    LimitEventType.USAGE_ADJUSTED, 100, 600, -500, LimitSource.NONE, null,
                    EventActor.systemUser(3L), null, null, EN_MARZO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("requires a written reason");
        }

        @Test
        @DisplayName("un motivo a medias —código sin texto— se rechaza en los demás hechos")
        void un_motivo_a_medias_se_rechaza() {
            assertThatThrownBy(() -> CompanyLimitEvent.record(ANA, EJE_ANIMAL,
                    LimitEventType.LIMIT_BLOCKED, 100, 100, 1, LimitSource.CATALOG_DEFAULT, null,
                    EventActor.employee(9L), "RETENTION", null, EN_MARZO))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("go together");
        }
    }

    @Nested
    @DisplayName("el trío del actor: exactamente uno")
    class Actor {

        @Test
        @DisplayName("un hecho sin ningún actor se rechaza")
        void un_hecho_sin_actor_se_rechaza() {
            assertThatThrownBy(() -> new EventActor(null, null, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exactly one actor");
        }

        @Test
        @DisplayName("un hecho con empleado y persona de plataforma a la vez se rechaza")
        void un_hecho_con_dos_actores_se_rechaza() {
            assertThatThrownBy(() -> new EventActor(9L, 3L, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("R-LIMIT-31 · OVER_LIMIT_ON_DOWNGRADE lo escribe el proceso, no el cliente")
        void bajar_el_techo_de_300_a_100_con_400_mascotas_escribe_OVER_LIMIT_ON_DOWNGRADE() {
            CompanyLimitEvent desbordado = CompanyLimitEvent.record(ANA, EJE_ANIMAL,
                    LimitEventType.OVER_LIMIT_ON_DOWNGRADE, 100, 400, 0, LimitSource.SUBSCRIPTION,
                    null, EventActor.automatedProcess(), null, null, EN_MARZO);

            assertThat(desbordado.getActor().process()).isTrue();
            assertThat(desbordado.getUsedQuantity()).isEqualTo(400);
            assertThat(desbordado.getLimitQuantity()).isEqualTo(100);
        }
    }
}
