package com.vetsoftware.app.platformaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.security.BCryptPasswordHasher;
import com.vetsoftware.app.infrastructure.security.PasswordHasher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El adaptador que le da a la feature el mismo bcrypt que usa el resto del
 * producto, en vez de un hash propio.
 *
 * <p>
 * <b>Por qué bcrypt y no el SHA-256 de los tokens.</b> El código de
 * verificación tiene <b>seis dígitos</b>: 10⁶ preimágenes. Un SHA-256 de eso se
 * invierte con una tabla que cabe en un portátil, así que quien lea la fila
 * —una copia de seguridad, un volcado, una consulta— tendría el código. El
 * coste por intento de bcrypt es lo que hace inviable esa tabla. Y su
 * comparación no cortocircuita en el primer byte distinto, que es lo que impide
 * deducir por latencia cuántos dígitos se acertaron.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BCryptSecretHasher — el código de 6 dígitos se guarda con bcrypt")
class BCryptSecretHasherTest {

    @Mock
    private PasswordHasher passwordHasher;

    @Nested
    @DisplayName("delegación")
    class Delegacion {

        @Test
        @DisplayName("hash delega en el hasher comun y devuelve lo que este produce")
        void hash_delega_en_el_hasher_comun() {
            when(passwordHasher.hash("123456")).thenReturn("$2a$10$resultado");

            String resultado = new BCryptSecretHasher(passwordHasher).hash("123456");

            assertThat(resultado).isEqualTo("$2a$10$resultado");
            verify(passwordHasher).hash("123456");
        }

        @Test
        @DisplayName("matches delega en el hasher comun: la comparacion no se reimplementa aqui")
        void matches_delega_en_el_hasher_comun() {
            when(passwordHasher.matches("123456", "$2a$10$almacenado")).thenReturn(true);

            assertThat(
                    new BCryptSecretHasher(passwordHasher).matches("123456", "$2a$10$almacenado"))
                    .isTrue();
        }

        @Test
        @DisplayName("un codigo que no casa devuelve false, no lanza")
        void un_codigo_que_no_casa_devuelve_false() {
            when(passwordHasher.matches("000000", "$2a$10$almacenado")).thenReturn(false);

            assertThat(
                    new BCryptSecretHasher(passwordHasher).matches("000000", "$2a$10$almacenado"))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("contra el bcrypt de verdad")
    class ConBcryptReal {

        private final BCryptSecretHasher hasher = new BCryptSecretHasher(
                new BCryptPasswordHasher());

        @Test
        @DisplayName("el hash del codigo NO es el codigo: la fila no lo revela")
        void el_hash_no_es_el_codigo() {
            String hash = hasher.hash("123456");

            assertThat(hash).isNotEqualTo("123456").doesNotContain("123456").startsWith("$2");
        }

        @Test
        @DisplayName("dos hashes del mismo codigo son distintos: la sal impide la tabla precalculada")
        void dos_hashes_del_mismo_codigo_son_distintos() {
            // Con SHA-256 los dos serian identicos y 10^6 entradas bastarian para
            // invertir cualquier codigo del sistema de una vez.
            assertThat(hasher.hash("123456")).isNotEqualTo(hasher.hash("123456"));
        }

        @Test
        @DisplayName("el codigo correcto casa contra su propio hash")
        void el_codigo_correcto_casa() {
            assertThat(hasher.matches("123456", hasher.hash("123456"))).isTrue();
        }

        @Test
        @DisplayName("un codigo distinto no casa, aunque solo cambie un digito")
        void un_codigo_distinto_no_casa() {
            assertThat(hasher.matches("123457", hasher.hash("123456"))).isFalse();
        }

        @Test
        @DisplayName("la cadena vacia con la que se compara un codigo nulo tampoco casa")
        void la_cadena_vacia_no_casa() {
            // PlatformAccessDecisions convierte el codigo nulo en "" antes de
            // comparar: tiene que fallar como cualquier otro codigo, no colarse.
            assertThat(hasher.matches("", hasher.hash("123456"))).isFalse();
        }
    }
}
