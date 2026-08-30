package com.vetsoftware.app.aiproposal.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El cupo de correo por destinatario. Lo que se prueba aqui es la <b>clave</b>
 * y la politica ante una caida de Valkey, no el consumo del cubo: eso vive en
 * Redis y no se afirma sin una base real.
 *
 * <p>
 * &#9940; <b>La clave lleva un hash, nunca el correo.</b> Una clave de Redis no
 * se anonimiza a los 90 dias ni se purga a los 24 meses: escribir alli la
 * direccion es sacar el dato personal justo del unico sitio donde la politica
 * de retencion lo alcanza. Y va normalizada a minusculas —la misma
 * normalizacion que aplica la columna generada {@code contact_email_hash}—,
 * porque si no, cambiar una mayuscula esquiva el limite entero.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValkeyProposalEmailThrottle — un correo por destinatario y por hora")
class ValkeyProposalEmailThrottleTest {

    @Mock
    private LettuceBasedProxyManager<String> proxyManager;

    @Mock
    private RemoteBucketBuilder<String> builder;

    @Mock
    private BucketProxy bucket;

    private ValkeyProposalEmailThrottle throttle;

    @BeforeEach
    void montar() {
        throttle = new ValkeyProposalEmailThrottle(proxyManager);
    }

    /**
     * {@code RemoteBucketBuilder.build} esta sobrecargado —uno recibe la
     * {@link BucketConfiguration} y otro un {@code Supplier} de ella—, asi que un
     * {@code any()} pelado no compila: el matcher tiene que decir cual de los dos.
     */
    private static Supplier<BucketConfiguration> cualquieraConfiguracion() {
        return any();
    }

    private void cubo(boolean quedaCupo) {
        when(proxyManager.builder()).thenReturn(builder);
        when(builder.build(anyString(), cualquieraConfiguracion())).thenReturn(bucket);
        when(bucket.tryConsume(1)).thenReturn(quedaCupo);
    }

    @Nested
    @DisplayName("La clave")
    class LaClave {

        @Test
        @DisplayName("no contiene el correo: lleva su SHA-256 detras del prefijo")
        void la_clave_no_contiene_el_correo() {
            cubo(true);

            throttle.tryAcquire("laura@vetchapinero.co");

            ArgumentCaptor<String> clave = ArgumentCaptor.forClass(String.class);
            org.mockito.Mockito.verify(builder).build(clave.capture(), cualquieraConfiguracion());
            assertThat(clave.getValue()).startsWith(ValkeyProposalEmailThrottle.KEY_PREFIX)
                    .doesNotContain("laura").doesNotContain("@")
                    .isEqualTo(ValkeyProposalEmailThrottle.KEY_PREFIX
                            + ValkeyProposalEmailThrottle.hash("laura@vetchapinero.co"));
        }

        /**
         * Sin normalizar, {@code Laura@X.CO} y {@code laura@x.co} tendrian cubos
         * distintos y el limite no filtraria nada: basta con alternar mayusculas.
         */
        @Test
        @DisplayName("el hash normaliza a minusculas, como la columna generada de la tabla")
        void el_hash_normaliza_a_minusculas() {
            assertThat(ValkeyProposalEmailThrottle.hash("Laura@VetChapinero.CO"))
                    .isEqualTo(ValkeyProposalEmailThrottle.hash("laura@vetchapinero.co"));
        }

        @Test
        @DisplayName("el hash es hexadecimal de 64 caracteres, que es SHA-256 completo")
        void el_hash_es_sha256_hex() {
            assertThat(ValkeyProposalEmailThrottle.hash("laura@vetchapinero.co")).hasSize(64)
                    .matches("[0-9a-f]{64}");
        }
    }

    @Nested
    @DisplayName("El cupo")
    class ElCupo {

        @Test
        @DisplayName("con cupo deja pasar")
        void con_cupo_deja_pasar() {
            cubo(true);

            assertThat(throttle.tryAcquire("laura@vetchapinero.co")).isTrue();
        }

        @Test
        @DisplayName("sin cupo corta")
        void sin_cupo_corta() {
            cubo(false);

            assertThat(throttle.tryAcquire("laura@vetchapinero.co")).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("un destinatario en blanco no consulta Valkey y no deja pasar")
        void un_destinatario_en_blanco_no_consulta(String correo) {
            assertThat(throttle.tryAcquire(correo)).isFalse();

            verifyNoInteractions(proxyManager);
        }

        /**
         * <b>Fail-open, y esta escrito a proposito.</b> El riesgo de no limitar durante
         * una caida de Redis es mandar correos de mas; el de fallar cerrado es que
         * ningun prospecto reciba su enlace mientras dure la incidencia, con la
         * propuesta ya cobrada al modelo y guardada. Es la decision opuesta a la del
         * tope de gasto —que si es fail-closed— porque lo que hay al otro lado es
         * distinto: alli, dinero.
         */
        @Test
        @DisplayName("si Valkey no responde deja pasar, al reves que el tope de gasto")
        void si_valkey_no_responde_deja_pasar() {
            when(proxyManager.builder()).thenThrow(new IllegalStateException("Valkey no responde"));

            assertThat(throttle.tryAcquire("laura@vetchapinero.co")).isTrue();
        }
    }
}
