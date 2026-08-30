package com.vetsoftware.app.aiproposal.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * La particion que decide el nivel del log, y el saneado que impide que un
 * codigo del proveedor fabrique una linea de log falsa.
 *
 * <p>
 * <b>Lo que se protege aqui no es el nombre de un enum, es que un
 * {@code AccessDeniedException} no se registre con el mismo nivel que un
 * 429.</b> Si los dos salen {@code WARN}, el fallo que rompe el 100 % de las
 * propuestas queda escondido detras del ruido del fallo que se cura solo, y el
 * producto lleva seis horas vendiendo sin IA con {@code level=error} limpio.
 */
@DisplayName("AiErrorType — la particion que decide ERROR contra WARN")
class AiErrorTypeTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"MODEL_ACCESS_NOT_ENABLED,model_access_not_enabled", "MODEL_TIMEOUT,timeout",
            "MODEL_RATE_LIMITED,rate_limited", "MODEL_FORBIDDEN,forbidden",
            "MODEL_UNEXPECTED_ERROR,unexpected_error", "MODEL_OUTPUT_UNREADABLE,output_unreadable"})
    @DisplayName("los codigos que el contrato de ModelInvoker declara se traducen a su error.type")
    void los_codigos_declarados_se_traducen(String failureCode, String esperado) {
        assertThat(AiErrorType.deFailureCode(failureCode).value()).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"THROTTLING_EXCEPTION", "algo raro", "", "   "})
    @DisplayName("un codigo que no esta en el vocabulario cae en _OTHER, que es SISTEMICO")
    void lo_desconocido_cae_en_other_y_es_sistemico(String failureCode) {
        AiErrorType tipo = AiErrorType.deFailureCode(failureCode);

        assertThat(tipo).isEqualTo(AiErrorType.OTHER);
        // Hacia el lado ruidoso a proposito: una rama que falta es una rama que
        // alguien tiene que escribir, y solo se entera si el contador de ERROR sube.
        assertThat(tipo.esSistemico()).isTrue();
    }

    @Test
    @DisplayName("un failureCode nulo no revienta el manejador de errores")
    void un_codigo_nulo_no_revienta() {
        assertThat(AiErrorType.deFailureCode(null)).isEqualTo(AiErrorType.OTHER);
        assertThat(AiErrorType.codigoSeguro(null)).isEqualTo(AiErrorType.DESCONOCIDO);
    }

    @Test
    @DisplayName("la caja no cambia el veredicto: forbidden y FORBIDDEN son el mismo hecho")
    void la_caja_no_cambia_el_veredicto() {
        assertThat(AiErrorType.deFailureCode("model_forbidden"))
                .isEqualTo(AiErrorType.MODEL_FORBIDDEN);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = AiErrorType.class, names = {"MODEL_TIMEOUT", "MODEL_CONNECTION_ERROR",
            "MODEL_RATE_LIMITED", "MODEL_SERVER_ERROR", "MODEL_OVERLOADED",
            "MODEL_OUTPUT_UNREADABLE"})
    @DisplayName("lo transitorio y lo aislado es WARN: existe camino de recuperacion ya implementado")
    void lo_aislado_no_es_sistemico(AiErrorType tipo) {
        assertThat(tipo.esSistemico()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = AiErrorType.class, names = {"MODEL_UNAUTHORIZED", "MODEL_FORBIDDEN",
            "MODEL_INVALID_REQUEST", "MODEL_ACCESS_NOT_ENABLED", "MODEL_UNEXPECTED_ERROR", "OTHER"})
    @DisplayName("lo determinista es ERROR: fallara el 100 % hasta que una persona cambie algo")
    void lo_determinista_es_sistemico(AiErrorType tipo) {
        assertThat(tipo.esSistemico()).isTrue();
    }

    @Test
    @DisplayName("NONE existe y no es un error: la etiqueta tiene que estar tambien en el camino feliz")
    void none_existe_y_no_es_error() {
        // Una etiqueta que solo aparece al fallar parte el medidor en dos juegos de
        // etiquetas distintos y las series dejan de sumarse entre si.
        assertThat(AiErrorType.NONE.value()).isEqualTo("none");
        assertThat(AiErrorType.NONE.esSistemico()).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(AiErrorType.class)
    @DisplayName("ningun error.type lleva mayusculas ni espacios: son valores de etiqueta")
    void todo_valor_es_una_etiqueta_valida(AiErrorType tipo) {
        assertThat(tipo.value()).isEqualTo(tipo.value().toLowerCase(Locale.ROOT))
                .doesNotContain(" ").isNotBlank();
    }

    @Test
    @DisplayName("un codigo con CRLF no puede fabricar una segunda linea de log (ASVS V7.3.1)")
    void un_codigo_con_crlf_no_inyecta() {
        String inyeccion = "OK\r\nlevel=INFO evento=todo_bien";

        assertThat(AiErrorType.codigoSeguro(inyeccion)).isEqualTo(AiErrorType.DESCONOCIDO)
                .doesNotContain("\n").doesNotContain("\r");
    }

    @Test
    @DisplayName("un codigo de mas de 40 caracteres tampoco pasa: no cabe en la columna ni en la etiqueta")
    void un_codigo_demasiado_largo_no_pasa() {
        assertThat(AiErrorType.codigoSeguro("A".repeat(41))).isEqualTo(AiErrorType.DESCONOCIDO);
        assertThat(AiErrorType.codigoSeguro("A".repeat(40))).hasSize(40);
    }

    @Test
    @DisplayName("un codigo con la forma acordada sale entero: sanear no puede cegar la investigacion")
    void un_codigo_bien_formado_sale_entero() {
        assertThat(AiErrorType.codigoSeguro("MODEL_TIMEOUT")).isEqualTo("MODEL_TIMEOUT");
        assertThat(AiErrorType.codigoSeguro(" model_timeout ")).isEqualTo("MODEL_TIMEOUT");
    }
}
