package com.vetsoftware.app.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("SubscriptionStatus - la politica de acceso")
class SubscriptionStatusTest {

    @Nested
    @DisplayName("R18 - no existe estado de corte total de acceso")
    class SinCorteTotal {

        /**
         * El test que hace de la politica algo verificable. Un cliente moroso nunca
         * puede quedarse sin poder consultar su propia historia clinica: es un riesgo
         * legal real. Si alguien anade un estado que niegue la lectura, este test lo
         * para.
         */
        @ParameterizedTest
        @EnumSource(SubscriptionStatus.class)
        @DisplayName("todos los estados permiten leer, sin excepcion")
        void todosPermitenLeer(SubscriptionStatus status) {
            assertThat(status.allowsRead())
                    .as("R18: %s no puede negar el acceso de lectura", status).isTrue();
        }

        @Test
        @DisplayName("el maximo de restriccion es READ_ONLY: puede leer y no puede escribir")
        void readOnlyEsElMaximo() {
            assertThat(SubscriptionStatus.READ_ONLY.allowsRead()).isTrue();
            assertThat(SubscriptionStatus.READ_ONLY.allowsWrite()).isFalse();
        }

        @Test
        @DisplayName("solo existen los seis estados especificados")
        void soloSeisEstados() {
            assertThat(SubscriptionStatus.values()).containsExactly(SubscriptionStatus.TRIALING,
                    SubscriptionStatus.ACTIVE, SubscriptionStatus.PAST_DUE,
                    SubscriptionStatus.READ_ONLY, SubscriptionStatus.CANCELLED,
                    SubscriptionStatus.EXPIRED);
        }
    }

    @Nested
    @DisplayName("El criterio de vigente")
    class Vigente {

        @Test
        @DisplayName("PAST_DUE sigue siendo vigente: debe, pero sigue trabajando")
        void pastDueEsVigente() {
            assertThat(SubscriptionStatus.PAST_DUE.isCurrent()).isTrue();
            assertThat(SubscriptionStatus.PAST_DUE.allowsWrite()).isTrue();
        }

        @Test
        @DisplayName("READ_ONLY sigue siendo vigente aunque no pueda escribir")
        void readOnlyEsVigente() {
            assertThat(SubscriptionStatus.READ_ONLY.isCurrent()).isTrue();
        }

        @Test
        @DisplayName("vigente NO es status = ACTIVE")
        void vigenteNoEsSoloActive() {
            assertThat(SubscriptionStatus.CURRENT).containsExactlyInAnyOrder(
                    SubscriptionStatus.TRIALING, SubscriptionStatus.ACTIVE,
                    SubscriptionStatus.PAST_DUE, SubscriptionStatus.READ_ONLY);
        }

        @Test
        @DisplayName("CANCELLED y EXPIRED salen del marcador y son terminales")
        void cancelledYExpiredSalen() {
            assertThat(SubscriptionStatus.CANCELLED.isCurrent()).isFalse();
            assertThat(SubscriptionStatus.EXPIRED.isCurrent()).isFalse();
            assertThat(SubscriptionStatus.CANCELLED.isTerminal()).isTrue();
            assertThat(SubscriptionStatus.EXPIRED.isTerminal()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(SubscriptionStatus.class)
        @DisplayName("isCurrent coincide exactamente con el conjunto CURRENT")
        void isCurrentCoincideConElConjunto(SubscriptionStatus status) {
            assertThat(status.isCurrent()).isEqualTo(SubscriptionStatus.CURRENT.contains(status));
        }
    }
}
