package com.vetsoftware.app.platformaccess.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * El suelo y el techo de la contraseña del superadministrador.
 *
 * <p>
 * La misma regla vive en dos sitios —{@code @Size(min = 12, max = 100)} en el
 * request y aquí—, y eso es deliberado: el request protege la forma del cuerpo
 * HTTP y esta clase protege al caso de uso de cualquier otro llamador. Que el
 * bean validation cubra hoy los dos extremos no autoriza a quitar este, que es
 * el que sigue vivo si mañana el flujo se invoca desde un job.
 */
@DisplayName("PlatformAccessPasswords — el suelo de la contraseña de plataforma")
class PlatformAccessPasswordsTest {

    @Test
    @DisplayName("una contrasena de longitud valida no lanza")
    void una_contrasena_valida_no_lanza() {
        assertThatCode(() -> PlatformAccessPasswords.require("contrasena-larga-1"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("exactamente 12 caracteres vale: el minimo es inclusivo")
    void exactamente_doce_caracteres_vale() {
        assertThatCode(() -> PlatformAccessPasswords.require("a".repeat(12)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("exactamente 100 caracteres vale: el maximo es inclusivo")
    void exactamente_cien_caracteres_vale() {
        assertThatCode(() -> PlatformAccessPasswords.require("a".repeat(100)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("once caracteres se rechaza: un caracter por debajo del suelo")
    void once_caracteres_se_rechaza() {
        assertThatThrownBy(() -> PlatformAccessPasswords.require("a".repeat(11)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 12 chars");
    }

    @Test
    @DisplayName("ciento uno se rechaza: bcrypt trunca a 72 bytes y un techo alto invita a pensar que no")
    void ciento_un_caracteres_se_rechaza() {
        assertThatThrownBy(() -> PlatformAccessPasswords.require("a".repeat(101)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100 chars or less");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t\n"})
    @DisplayName("vacia o solo espacios se rechaza como ausente, no como corta")
    void vacia_o_en_blanco_se_rechaza_como_ausente(String valor) {
        assertThatThrownBy(() -> PlatformAccessPasswords.require(valor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password is required");
    }

    @Test
    @DisplayName("nula se rechaza como ausente")
    void nula_se_rechaza_como_ausente() {
        assertThatThrownBy(() -> PlatformAccessPasswords.require(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password is required");
    }
}
