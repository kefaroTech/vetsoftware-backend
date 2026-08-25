package com.vetsoftware.app.platformaccess.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Los dos secretos del flujo y el digest con el que se guardan.
 *
 * <p>
 * <b>El token va con SHA-256 y el código con bcrypt, y la diferencia no es un
 * descuido.</b> El token tiene 256 bits de entropía: no hay tabla que lo
 * invierta, y hacerlo con bcrypt costaría el trabajo de un hash lento en cada
 * validación de enlace. El código tiene 10⁶ preimágenes y con SHA-256 sería
 * trivialmente invertible desde una copia de la fila, de ahí que ese sí lleve
 * bcrypt ({@code BCryptSecretHasher}).
 *
 * <p>
 * Lo que se fija aquí: que el token no sea adivinable, que el código tenga
 * siempre seis dígitos —incluidos los que empiezan por cero, que un
 * {@code String.valueOf} convertiría en cinco— y que el digest sea hex en
 * minúsculas de 64 caracteres, que es exactamente lo que la columna
 * {@code VARCHAR(64)} y el {@code UNIQUE} esperan.
 */
@DisplayName("PlatformAccessTokens — los dos secretos y su digest")
class PlatformAccessTokensTest {

    @Nested
    @DisplayName("token del enlace")
    class Token {

        @Test
        @DisplayName("es url-safe y sin relleno: viaja en un query param sin escapar")
        void es_url_safe_y_sin_relleno() {
            String token = PlatformAccessTokens.generateRawToken();

            assertThat(token).doesNotContain("+").doesNotContain("/").doesNotContain("=")
                    .matches("[A-Za-z0-9_-]+");
        }

        @Test
        @DisplayName("mide 43 caracteres: 32 bytes en base64 sin relleno")
        void mide_cuarenta_y_tres_caracteres() {
            // 256 bits de entropia. Con menos, el enlace del aprobador entra en el
            // terreno de lo enumerable y el codigo de 6 digitos deja de ser el
            // segundo obstaculo para ser el unico.
            assertThat(PlatformAccessTokens.generateRawToken()).hasSize(43);
        }

        @Test
        @DisplayName("dos tokens seguidos nunca coinciden")
        void dos_tokens_seguidos_nunca_coinciden() {
            assertThat(
                    Stream.generate(PlatformAccessTokens::generateRawToken).limit(200).distinct())
                    .hasSize(200);
        }
    }

    @Nested
    @DisplayName("código de verificación")
    class Codigo {

        @Test
        @DisplayName("siempre seis digitos, incluidos los que empiezan por cero")
        void siempre_seis_digitos() {
            // El %06d es lo que impide que el 4271 salga como «4271» y el front lo
            // rechace por su @Pattern de seis digitos exactos.
            assertThat(IntStream.range(0, 500)
                    .mapToObj(i -> PlatformAccessTokens.generateVerificationCode()))
                    .allMatch(codigo -> codigo.matches("\\d{6}"));
        }

        @Test
        @DisplayName("no siempre el mismo: 500 tiradas dan mas de un valor")
        void no_siempre_el_mismo() {
            assertThat(Stream.generate(PlatformAccessTokens::generateVerificationCode).limit(500)
                    .distinct()).hasSizeGreaterThan(1);
        }
    }

    @Nested
    @DisplayName("digest")
    class Digest {

        @Test
        @DisplayName("es SHA-256 en hex minusculas de 64 caracteres, que es lo que mide la columna")
        void es_hex_minusculas_de_sesenta_y_cuatro() {
            assertThat(PlatformAccessTokens.hash("token-plano-de-prueba")).hasSize(64)
                    .matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("el valor es el SHA-256 conocido de la cadena: si alguien cambia el algoritmo, cae")
        void el_valor_es_el_sha256_conocido() {
            // SHA-256 de "abc". Fijar el valor y no solo la forma es lo que convierte
            // este caso en una alarma: cambiar el digest invalidaria en silencio
            // todos los tokens ya emitidos, que solo existen como hash en la base.
            assertThat(PlatformAccessTokens.hash("abc"))
                    .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        }

        @Test
        @DisplayName("el mismo token da siempre el mismo digest: sin sal, porque hay que buscar por el")
        void el_mismo_token_da_el_mismo_digest() {
            assertThat(PlatformAccessTokens.hash("abc"))
                    .isEqualTo(PlatformAccessTokens.hash("abc"));
        }

        @Test
        @DisplayName("un token distinto da un digest distinto")
        void un_token_distinto_da_un_digest_distinto() {
            assertThat(PlatformAccessTokens.hash("abc"))
                    .isNotEqualTo(PlatformAccessTokens.hash("abd"));
        }
    }
}
