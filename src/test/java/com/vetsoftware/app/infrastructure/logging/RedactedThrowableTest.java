package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link RedactedThrowable} es package-private: se prueba directamente su único
 * punto de entrada, {@link RedactedThrowable#redact(IThrowableProxy)}, sin
 * pasar por el pipeline completo de Logback (eso ya lo cubre
 * {@code LogRedactionPipelineTest}).
 */
class RedactedThrowableTest {

    private static final String DECOY = "SENUELO-No-Debe-Salir-9c2b";

    @Nested
    @DisplayName("casos que no tocan el proxy original")
    class SinCambios {

        @Test
        @DisplayName("un proxy nulo se devuelve tal cual")
        void un_proxy_nulo_se_devuelve_tal_cual() {
            assertThat(RedactedThrowable.redact(null)).isNull();
        }

        @Test
        @DisplayName("un proxy que no es ThrowableProxy se devuelve sin tocar")
        void un_proxy_que_no_es_throwable_proxy_se_devuelve_sin_tocar() {
            IThrowableProxy foreignProxy = mock(IThrowableProxy.class);

            assertThat(RedactedThrowable.redact(foreignProxy)).isSameAs(foreignProxy);
        }

        @Test
        @DisplayName("una excepción sin datos sensibles conserva el mismo proxy")
        void una_excepcion_limpia_conserva_el_mismo_proxy() {
            ThrowableProxy proxy = new ThrowableProxy(
                    new IllegalStateException("estado inválido de la sesión de caja"));

            assertThat(RedactedThrowable.redact(proxy)).isSameAs(proxy);
        }
    }

    @Nested
    @DisplayName("redacción de la cadena de causas")
    class ConCambios {

        @Test
        @DisplayName("un mensaje sensible se redacta conservando el tipo original en toString")
        void un_mensaje_sensible_se_redacta_conservando_el_tipo_original() {
            ThrowableProxy proxy = new ThrowableProxy(
                    new IllegalStateException("password=" + DECOY));

            ThrowableProxy redacted = (ThrowableProxy) RedactedThrowable.redact(proxy);

            assertThat(redacted).isNotSameAs(proxy);
            assertThat(redacted.getMessage()).isEqualTo("password=***");
            // getClassName() refleja la clase real (RedactedThrowable); el tipo original
            // sobrevive en toString(), que es lo que Logback usa como overridingMessage.
            assertThat(redacted.getThrowable().toString())
                    .isEqualTo(IllegalStateException.class.getName() + ": password=***");
        }

        @Test
        @DisplayName("un mensaje nulo cuya cadena cambia por otro motivo se representa solo con el tipo")
        void un_mensaje_nulo_se_representa_solo_con_el_tipo_original() {
            // El wrapper no tiene mensaje propio, pero su suprimida sí lleva un secreto:
            // igualmente se copia porque la cadena cambió, y toString() debe caer en la
            // rama sin mensaje.
            Throwable wrapper = new IllegalStateException();
            wrapper.addSuppressed(new IllegalArgumentException("clave=" + DECOY));
            ThrowableProxy proxy = new ThrowableProxy(wrapper);

            IThrowableProxy redacted = RedactedThrowable.redact(proxy);

            assertThat(redacted).isNotSameAs(proxy);
            assertThat(redacted.getMessage()).isNull();
            assertThat(redacted.getSuppressed()[0].getMessage()).isEqualTo("clave=***");
        }

        @Test
        @DisplayName("una cadena de causas cíclica no produce un bucle infinito")
        void una_cadena_ciclica_no_produce_un_bucle_infinito() {
            Throwable a = new Throwable("password=" + DECOY);
            Throwable b = new Throwable("token=" + DECOY, a);
            a.initCause(b);

            IThrowableProxy redacted = RedactedThrowable.redact(new ThrowableProxy(a));

            assertThat(redacted.getMessage()).isEqualTo("password=***");
            assertThat(redacted.getCause().getMessage()).isEqualTo("token=***");
        }
    }
}
