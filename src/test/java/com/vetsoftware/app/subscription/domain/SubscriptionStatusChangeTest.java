package com.vetsoftware.app.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * La bitacora que explica por que una cuenta esta como esta. Es append-only: no
 * se corrige, se anota otra fila.
 */
@DisplayName("SubscriptionStatusChange - la pelicula del contrato")
class SubscriptionStatusChangeTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final LocalDateTime MOMENTO = LocalDateTime.of(2026, 3, 10, 9, 30, 15,
            123456000);

    private static SubscriptionStatusChange anotacion(Long companyId, SubscriptionStatus desde,
            SubscriptionStatus hacia, String motivo, LocalDateTime ocurrioEn, String actor) {
        return SubscriptionStatusChange.record(companyId, CONTRATO, desde, hacia, motivo, actor,
                ocurrioEn);
    }

    @Nested
    @DisplayName("Que se anota")
    class QueSeAnota {

        @Test
        @DisplayName("guarda de donde venia, a donde va, por que y quien lo hizo")
        void guardaLaTransicionCompleta() {
            SubscriptionStatusChange anotacion = anotacion(EMPRESA, SubscriptionStatus.ACTIVE,
                    SubscriptionStatus.PAST_DUE, "Cuota de marzo vencida", MOMENTO, "cobranza");

            assertThat(anotacion.getCompanyId()).isEqualTo(EMPRESA);
            assertThat(anotacion.getSubscriptionId()).isEqualTo(CONTRATO);
            assertThat(anotacion.getFromStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(anotacion.getToStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
            assertThat(anotacion.getReason()).isEqualTo("Cuota de marzo vencida");
            assertThat(anotacion.getActor()).isEqualTo("cobranza");
            assertThat(anotacion.getOccurredAt()).isEqualTo(MOMENTO);
        }

        @Test
        @DisplayName("conserva los microsegundos: dos transiciones del mismo segundo se ordenan")
        void conservaLosMicrosegundos() {
            // La columna es DATETIME(6) por esto. Si el instante se truncara al
            // segundo, la pelicula de un contrato que pasa a PAST_DUE y a READ_ONLY en
            // el mismo segundo se leeria al reves y la respuesta a «por que se
            // restringio esta cuenta» saldria invertida.
            SubscriptionStatusChange anotacion = anotacion(EMPRESA, SubscriptionStatus.ACTIVE,
                    SubscriptionStatus.PAST_DUE, null, MOMENTO, "cobranza");

            assertThat(anotacion.getOccurredAt().getNano()).isEqualTo(123456000);
        }

        @Test
        @DisplayName("la primera fila no viene de ningun estado: fromStatus nulo es legitimo")
        void laPrimeraFilaNoVieneDeNingunEstado() {
            SubscriptionStatusChange alta = anotacion(EMPRESA, null, SubscriptionStatus.TRIALING,
                    "Alta", MOMENTO, "plataforma");

            assertThat(alta.getFromStatus()).isNull();
            assertThat(alta.getToStatus()).isEqualTo(SubscriptionStatus.TRIALING);
        }

        @Test
        @DisplayName("la anotacion nueva no lleva id ni fecha de creacion: los pone la base")
        void nuevaSinIdNiFecha() {
            SubscriptionStatusChange anotacion = anotacion(EMPRESA, SubscriptionStatus.ACTIVE,
                    SubscriptionStatus.PAST_DUE, null, MOMENTO, "cobranza");

            assertThat(anotacion.getId()).isNull();
            assertThat(anotacion.getCreatedDate()).isNull();
        }

        @Test
        @DisplayName("el motivo puede faltar: no toda transicion se explica con palabras")
        void motivoOpcional() {
            assertThat(anotacion(EMPRESA, SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE,
                    null, MOMENTO, "facturacion").getReason()).isNull();
        }
    }

    @Nested
    @DisplayName("Lo que no se puede anotar")
    class LoQueNoSePuedeAnotar {

        @Test
        @DisplayName("de un estado al mismo estado: la fila de ruido ensucia la pelicula")
        void mismoEstadoDeOrigenYDestino() {
            // chk_ssh_change. Sin esta guarda, un job idempotente que reaplica la misma
            // transicion llena la bitacora de filas «de ACTIVE a ACTIVE» y encontrar la
            // transicion que de verdad importa deja de ser posible a simple vista.
            assertThatThrownBy(() -> anotacion(EMPRESA, SubscriptionStatus.ACTIVE,
                    SubscriptionStatus.ACTIVE, "reintento", MOMENTO, "job"))
                    .isInstanceOf(InvalidSubscriptionStatusTransitionException.class);
        }

        static Stream<Arguments> anotacionesInvalidas() {
            return Stream.of(
                    Arguments.of("sin empresa",
                            (ThrowingCallable) () -> anotacion(null, SubscriptionStatus.ACTIVE,
                                    SubscriptionStatus.PAST_DUE, null, MOMENTO, "cobranza"),
                            "companyId"),
                    Arguments.of("sin estado destino",
                            (ThrowingCallable) () -> anotacion(EMPRESA, SubscriptionStatus.ACTIVE,
                                    null, null, MOMENTO, "cobranza"),
                            "toStatus"),
                    Arguments.of("motivo mas largo que la columna",
                            (ThrowingCallable) () -> anotacion(EMPRESA, SubscriptionStatus.ACTIVE,
                                    SubscriptionStatus.PAST_DUE, "M".repeat(256), MOMENTO,
                                    "cobranza"),
                            "reason must be 255"),
                    Arguments.of("sin instante",
                            (ThrowingCallable) () -> anotacion(EMPRESA, SubscriptionStatus.ACTIVE,
                                    SubscriptionStatus.PAST_DUE, null, null, "cobranza"),
                            "occurredAt"),
                    Arguments.of("sin actor",
                            (ThrowingCallable) () -> anotacion(EMPRESA, SubscriptionStatus.ACTIVE,
                                    SubscriptionStatus.PAST_DUE, null, MOMENTO, "  "),
                            "actor is required"),
                    Arguments.of("actor mas largo que la columna",
                            (ThrowingCallable) () -> anotacion(EMPRESA, SubscriptionStatus.ACTIVE,
                                    SubscriptionStatus.PAST_DUE, null, MOMENTO, "A".repeat(121)),
                            "actor must be 120"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("anotacionesInvalidas")
        @DisplayName("la anotacion rechaza lo mismo que rechazaria la base")
        void anotacionesInvalidas(String caso, ThrowingCallable creacion, String fragmento) {
            assertThatThrownBy(creacion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(fragmento);
        }
    }
}
