package com.vetsoftware.app.passwordreset.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PasswordResetTokens — generacion y hashing del token de restablecimiento")
class PasswordResetTokensTest {

    @Nested
    @DisplayName("generateRawToken")
    class GenerateRawToken {

        @Test
        @DisplayName("genera un valor Base64 URL-safe sin padding de 32 bytes")
        void genera_un_valor_base64_url_safe_de_32_bytes() {
            String raw = PasswordResetTokens.generateRawToken();

            assertThat(raw).doesNotContain("+", "/", "=");
            assertThat(Base64.getUrlDecoder().decode(raw)).hasSize(32);
        }

        @Test
        @DisplayName("cada llamada genera un valor distinto")
        void cada_llamada_genera_un_valor_distinto() {
            String primero = PasswordResetTokens.generateRawToken();
            String segundo = PasswordResetTokens.generateRawToken();

            assertThat(primero).isNotEqualTo(segundo);
        }
    }

    @Nested
    @DisplayName("hash")
    class Hash {

        @Test
        @DisplayName("SHA-256 en hex de 64 caracteres, contra un vector conocido")
        void sha256_en_hex_contra_un_vector_conocido() {
            assertThat(PasswordResetTokens.hash("test"))
                    .isEqualTo("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
        }

        @Test
        @DisplayName("es determinista: el mismo texto siempre da el mismo hash")
        void es_determinista() {
            assertThat(PasswordResetTokens.hash("mismo-valor"))
                    .isEqualTo(PasswordResetTokens.hash("mismo-valor"));
        }

        @Test
        @DisplayName("entradas distintas producen hashes distintos")
        void entradas_distintas_producen_hashes_distintos() {
            assertThat(PasswordResetTokens.hash("valor-a"))
                    .isNotEqualTo(PasswordResetTokens.hash("valor-b"));
        }
    }
}
