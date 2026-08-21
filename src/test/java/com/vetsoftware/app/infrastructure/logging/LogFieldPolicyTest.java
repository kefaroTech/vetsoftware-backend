package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

        /**
         * Incidencia #216. Un código de acceso lo teclea un humano, así que el sistema
         * no garantiza su forma: en el auto-registro <b>es</b> el correo del dueño.
         * Devolver estas dos claves a {@code VERBATIM} republica ese correo en claro en
         * {@code company_registered} y en cada {@code login_success}, y esta aserción
         * es lo que lo impide en el momento de escribirlo, sin esperar a que alguien
         * lea Loki.
         */
        @ParameterizedTest
        @ValueSource(strings = {"actor.identifier", "employee.identifier"})
        @DisplayName("un identificador cuya forma elige el usuario nunca es verbatim")
        void un_identificador_elegido_por_el_usuario_no_es_verbatim(String key) {
            assertThat(LogFieldPolicy.isVerbatim(key)).isFalse();
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

        /**
         * La otra mitad de #216: no basta con sacarlas de {@code VERBATIM}. Si quedaran
         * fuera de los dos niveles saldrían como {@code ***} enteras —el campo se
         * pierde y la investigación se queda sin actor, que es justo lo que el arreglo
         * no debía hacer—.
         */
        @ParameterizedTest
        @ValueSource(strings = {"actor.identifier", "employee.identifier"})
        @DisplayName("un identificador cuya forma elige el usuario se escanea, no se suprime")
        void un_identificador_elegido_por_el_usuario_se_escanea(String key) {
            assertThat(LogFieldPolicy.isScanned(key)).isTrue();
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
