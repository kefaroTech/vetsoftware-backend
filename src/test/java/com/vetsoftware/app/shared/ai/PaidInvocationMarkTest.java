package com.vetsoftware.app.shared.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * ⛔ <b>Los tres estados de la marca, y el que decide si el arreglo sirve de
 * algo es el tercero.</b> Sin marca se cobra: si eso se invirtiera —o si
 * alguien escribiera la lectura como {@code !Boolean.TRUE.equals(...)}— toda
 * petición del mundo entraría en la rama de devolución y el cupo diario pasaría
 * a ser decorativo, en silencio y con el resto de pruebas en verde.
 */
@DisplayName("PaidInvocationMark — la marca que cruza de la rodaja del asistente a la del cupo")
class PaidInvocationMarkTest {

    @Nested
    @DisplayName("Los tres estados")
    class TresEstados {

        @Test
        @DisplayName("sin marca NO consta que faltara la invocación: el estado por defecto es"
                + " cobrar")
        void sin_marca_no_consta_nada() {
            assertThat(PaidInvocationMark.constaQueNoHuboInvocacion(new MockHttpServletRequest()))
                    .as("un atributo ausente no puede leerse como «devuelve el cupo»").isFalse();
        }

        @Test
        @DisplayName("marcada como invocada tampoco: se pagó, se cobra")
        void marcada_como_invocada_no_devuelve() {
            MockHttpServletRequest peticion = new MockHttpServletRequest();

            PaidInvocationMark.marcar(peticion, true);

            assertThat(PaidInvocationMark.constaQueNoHuboInvocacion(peticion)).isFalse();
        }

        @Test
        @DisplayName("solo la marca explícita de «no hubo invocación» devuelve el cupo")
        void solo_la_marca_explicita_devuelve() {
            MockHttpServletRequest peticion = new MockHttpServletRequest();

            PaidInvocationMark.marcar(peticion, false);

            assertThat(PaidInvocationMark.constaQueNoHuboInvocacion(peticion)).isTrue();
            assertThat(peticion.getAttribute(PaidInvocationMark.ATRIBUTO)).isEqualTo(Boolean.FALSE);
        }
    }

    @Nested
    @DisplayName("Bordes")
    class Bordes {

        @Test
        @DisplayName("la última marca gana: un desenlace no queda tapado por el anterior")
        void la_ultima_marca_gana() {
            MockHttpServletRequest peticion = new MockHttpServletRequest();

            PaidInvocationMark.marcar(peticion, false);
            PaidInvocationMark.marcar(peticion, true);

            assertThat(PaidInvocationMark.constaQueNoHuboInvocacion(peticion)).isFalse();
        }

        /**
         * El caso real es el adaptador cuando no hay petición ligada al hilo. Ni
         * escribir ni leer puede reventar ahí: sería convertir un 200 en un 500 por un
         * dato de contabilidad.
         */
        @Test
        @DisplayName("sin petición no lanza, y el veredicto sigue siendo «cobra»")
        void sin_peticion_no_lanza() {
            PaidInvocationMark.marcar(null, false);

            assertThat(PaidInvocationMark.constaQueNoHuboInvocacion(null)).isFalse();
        }

        @Test
        @DisplayName("un valor ajeno en el atributo no se confunde con la marca")
        void un_valor_ajeno_no_se_confunde() {
            MockHttpServletRequest peticion = new MockHttpServletRequest();
            peticion.setAttribute(PaidInvocationMark.ATRIBUTO, "false");

            assertThat(PaidInvocationMark.constaQueNoHuboInvocacion(peticion))
                    .as("la cadena \"false\" no es la marca: solo el Boolean lo es").isFalse();
        }
    }
}
