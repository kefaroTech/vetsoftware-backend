package com.vetsoftware.app.registration.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * {@code VerificationTokens} es package-private: el test vive en el mismo
 * paquete.
 */
@DisplayName("VerificationTokens")
class VerificationTokensTest {

    @Test
    @DisplayName("genera un token distinto en cada llamada")
    void genera_tokens_distintos_en_cada_llamada() {
        String primero = VerificationTokens.generateRawToken();
        String segundo = VerificationTokens.generateRawToken();

        assertThat(primero).isNotEqualTo(segundo);
    }

    @Test
    @DisplayName("el token generado va en Base64 URL-safe sin padding")
    void el_token_generado_no_tiene_padding_base64() {
        String token = VerificationTokens.generateRawToken();

        assertThat(token).doesNotContain("=", "+", "/");
    }

    @Test
    @DisplayName("el hash tiene 64 caracteres hexadecimales (SHA-256)")
    void el_hash_tiene_64_caracteres_hexadecimales() {
        String hash = VerificationTokens.hash("valor-cualquiera");

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("el mismo valor produce siempre el mismo hash")
    void el_mismo_valor_produce_siempre_el_mismo_hash() {
        assertThat(VerificationTokens.hash("mismo-valor"))
                .isEqualTo(VerificationTokens.hash("mismo-valor"));
    }

    @Test
    @DisplayName("valores distintos producen hashes distintos")
    void valores_distintos_producen_hashes_distintos() {
        assertThat(VerificationTokens.hash("valor-a"))
                .isNotEqualTo(VerificationTokens.hash("valor-b"));
    }

    @Test
    @DisplayName("si el JDK no ofreciera SHA-256, se traduce en IllegalStateException")
    void si_el_jdk_no_ofreciera_sha256_se_traduce_en_illegal_state_exception() {
        // SHA-256 es obligatorio en toda implementacion de la JVM, asi que esta rama de
        // defensa nunca se dispara en produccion; se fuerza aqui mockeando el metodo
        // estatico para probar la traduccion de la excepcion (mismo patron que
        // Sha256RefreshTokenSecretTest.si_el_jdk_no_ofreciera_sha256_se_traduce_en_illegal_state_exception).
        try (MockedStatic<MessageDigest> digest = mockStatic(MessageDigest.class)) {
            digest.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("sin proveedor"));

            assertThatThrownBy(() -> VerificationTokens.hash("cualquiera"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SHA-256 not available");
        }
    }
}
