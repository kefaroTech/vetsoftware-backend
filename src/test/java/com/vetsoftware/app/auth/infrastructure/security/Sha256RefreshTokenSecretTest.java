package com.vetsoftware.app.auth.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Sin puertos que mockear: {@code SecureRandom} y {@code MessageDigest} son
 * colaboradores puros de la JDK.
 */
class Sha256RefreshTokenSecretTest {

    private final Sha256RefreshTokenSecret secret = new Sha256RefreshTokenSecret();

    @Nested
    @DisplayName("generateRaw")
    class GenerarCrudo {

        @Test
        @DisplayName("produce 64 caracteres hexadecimales: 32 bytes de entropía")
        void produce_64_caracteres_hexadecimales() {
            String raw = secret.generateRaw();

            assertThat(raw).hasSize(64).matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("dos llamadas sucesivas no repiten el mismo valor")
        void dos_llamadas_no_repiten_valor() {
            assertThat(secret.generateRaw()).isNotEqualTo(secret.generateRaw());
        }
    }

    @Nested
    @DisplayName("hash")
    class Hashear {

        @Test
        @DisplayName("coincide con el vector conocido de SHA-256 para la cadena vacía")
        void coincide_con_el_vector_conocido_de_la_cadena_vacia() {
            assertThat(secret.hash(""))
                    .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        }

        @Test
        @DisplayName("es determinista: el mismo crudo produce siempre el mismo hash")
        void es_determinista() {
            String raw = "un-token-cualquiera";

            assertThat(secret.hash(raw)).isEqualTo(secret.hash(raw));
        }

        @Test
        @DisplayName("crudos distintos producen hashes distintos")
        void crudos_distintos_producen_hashes_distintos() {
            assertThat(secret.hash("a")).isNotEqualTo(secret.hash("b"));
        }

        @Test
        @DisplayName("el hash nunca es el propio valor en claro")
        void el_hash_nunca_es_el_valor_en_claro() {
            assertThat(secret.hash("secreto")).isNotEqualTo("secreto").hasSize(64);
        }

        @Test
        @DisplayName("si el JDK no ofreciera SHA-256, se traduce en IllegalStateException")
        void si_el_jdk_no_ofreciera_sha256_se_traduce_en_illegal_state_exception() {
            // SHA-256 es obligatorio en toda implementación de la JVM, así que esta rama
            // de defensa nunca se dispara en producción; se fuerza aquí mockeando el
            // método estático para probar la traducción de la excepción. Mismo patrón
            // que VerificationTokensTest, que cubre este catch calcado en
            // VerificationTokens.
            try (MockedStatic<MessageDigest> digest = mockStatic(MessageDigest.class)) {
                digest.when(() -> MessageDigest.getInstance("SHA-256"))
                        .thenThrow(new NoSuchAlgorithmException("sin proveedor"));

                assertThatThrownBy(() -> secret.hash("cualquiera"))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("SHA-256 no disponible");
            }
        }
    }
}
