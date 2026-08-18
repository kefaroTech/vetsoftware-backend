package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LogFieldPolicyTest {

    @Nested
    @DisplayName("isVerbatim")
    class IsVerbatim {

        @Test
        @DisplayName("una clave declarada como verbatim se reconoce como tal")
        void reconoce_una_clave_verbatim_declarada() {
            assertThat(LogFieldPolicy.isVerbatim(MdcKeys.ACTOR_EMPLOYEE_ID)).isTrue();
        }

        @Test
        @DisplayName("una clave escaneada no cuenta como verbatim")
        void una_clave_escaneada_no_es_verbatim() {
            assertThat(LogFieldPolicy.isVerbatim(MdcKeys.HTTP_PATH)).isFalse();
        }

        @Test
        @DisplayName("una clave nula no es verbatim")
        void una_clave_nula_no_es_verbatim() {
            assertThat(LogFieldPolicy.isVerbatim(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("isScanned")
    class IsScanned {

        @Test
        @DisplayName("una clave declarada como escaneada se reconoce como tal")
        void reconoce_una_clave_escaneada_declarada() {
            assertThat(LogFieldPolicy.isScanned(MdcKeys.HTTP_PATH)).isTrue();
        }

        @Test
        @DisplayName("una clave verbatim no cuenta como escaneada")
        void una_clave_verbatim_no_es_escaneada() {
            assertThat(LogFieldPolicy.isScanned(MdcKeys.ACTOR_EMPLOYEE_ID)).isFalse();
        }

        @Test
        @DisplayName("una clave nula no es escaneada")
        void una_clave_nula_no_es_escaneada() {
            assertThat(LogFieldPolicy.isScanned(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("isAllowed")
    class IsAllowed {

        @Test
        @DisplayName("una clave verbatim está permitida")
        void una_clave_verbatim_esta_permitida() {
            assertThat(LogFieldPolicy.isAllowed(MdcKeys.ACTOR_EMPLOYEE_ID)).isTrue();
        }

        @Test
        @DisplayName("una clave escaneada está permitida")
        void una_clave_escaneada_esta_permitida() {
            assertThat(LogFieldPolicy.isAllowed(MdcKeys.HTTP_PATH)).isTrue();
        }

        @Test
        @DisplayName("una clave no declarada en ninguno de los dos niveles no está permitida")
        void una_clave_no_declarada_no_esta_permitida() {
            assertThat(LogFieldPolicy.isAllowed("owner.diagnosis")).isFalse();
        }

        @Test
        @DisplayName("una clave nula no está permitida")
        void una_clave_nula_no_esta_permitida() {
            assertThat(LogFieldPolicy.isAllowed(null)).isFalse();
        }
    }
}
