package com.vetsoftware.app.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BE-07: el cost de BCrypt sube de 10 a 12.
 *
 * <p>
 * Lo que se fija aquí es que ese cambio no invalida credenciales: si subir el
 * cost rompiera la verificación de los hashes ya guardados, nadie podría entrar
 * tras el despliegue y el fallo solo aparecería en produccion.
 */
class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void las_credenciales_guardadas_con_el_cost_anterior_siguen_verificando() {
        String hashCost10 = new BCryptPasswordEncoder(10).encode("Orlando1997*");

        assertThat(hasher.matches("Orlando1997*", hashCost10)).isTrue();
        assertThat(hasher.matches("otra-cosa", hashCost10)).isFalse();
    }

    @Test
    void los_hashes_nuevos_usan_el_cost_endurecido() {
        String hash = hasher.hash("Orlando1997*");

        // El cost viaja dentro del propio hash: $2a$<cost>$<salt+digest>
        assertThat(hash).startsWith("$2a$12$");
        assertThat(hasher.matches("Orlando1997*", hash)).isTrue();
    }

    @Test
    void un_hash_ausente_o_vacio_no_verifica_nunca() {
        assertThat(hasher.matches("Orlando1997*", null)).isFalse();
        assertThat(hasher.matches("Orlando1997*", "  ")).isFalse();
    }
}
