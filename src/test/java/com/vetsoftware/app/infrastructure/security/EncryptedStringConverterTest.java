package com.vetsoftware.app.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Cifrado de columnas sensibles (credenciales del proveedor DIAN). Lo que aqui
 * se fija no es "que cifre", sino tres cosas que si se rompen no avisan:
 * <ul>
 * <li>que descifrar lo cifrado devuelva el original — un fallo aqui deja las
 * credenciales del proveedor irrecuperables, y solo se descubre el dia que la
 * DIAN rechaza por autenticacion;</li>
 * <li>que dos cifrados del mismo texto NO sean iguales — si el IV se reutiliza,
 * AES-GCM deja de proteger y el patron es visible en la propia columna;</li>
 * <li>que el prefijo de version viaje con el dato, que es lo que permite rotar
 * la clave sin re-cifrarlo todo de golpe.</li>
 * </ul>
 *
 * <p>
 * La clave la inyecta surefire en {@code DIAN_ENC_KEY} (ver el pom): el
 * converter la lee del entorno de forma estatica porque lo instancia Hibernate,
 * no Spring.
 */
@DisplayName("EncryptedStringConverter — cifrado de columnas")
class EncryptedStringConverterTest {

    private final EncryptedStringConverter converter = new EncryptedStringConverter();

    @Nested
    @DisplayName("ida y vuelta")
    class IdaYVuelta {

        @ParameterizedTest(name = "\"{0}\"")
        @ValueSource(strings = {"clave-dian-123", "a", "  espacios  ",
                "acentos y ñ, símbolos: €@#$%", "{\"json\":\"anidado\"}",
                "0123456789012345678901234567890123456789012345678901234567890123456789"})
        @DisplayName("descifrar lo cifrado devuelve exactamente el original")
        void descifrar_lo_cifrado_devuelve_el_original(String original) {
            String cifrado = converter.convertToDatabaseColumn(original);

            assertThat(converter.convertToEntityAttribute(cifrado)).isEqualTo(original);
        }

        @Test
        @DisplayName("el texto cifrado no contiene el original en claro")
        void el_texto_cifrado_no_contiene_el_original() {
            String cifrado = converter.convertToDatabaseColumn("clave-dian-123");

            assertThat(cifrado).doesNotContain("clave-dian-123");
        }

        @Test
        @DisplayName("una cadena vacia se cifra y se recupera vacia")
        void una_cadena_vacia_se_cifra_y_se_recupera() {
            String cifrado = converter.convertToDatabaseColumn("");

            assertThat(converter.convertToEntityAttribute(cifrado)).isEmpty();
        }
    }

    @Nested
    @DisplayName("nulos")
    class Nulos {

        @Test
        @DisplayName("null se persiste como null, no como cadena cifrada")
        void null_se_persiste_como_null() {
            assertThat(converter.convertToDatabaseColumn(null)).isNull();
        }

        @Test
        @DisplayName("null en base se lee como null")
        void null_en_base_se_lee_como_null() {
            assertThat(converter.convertToEntityAttribute(null)).isNull();
        }
    }

    @Nested
    @DisplayName("propiedades criptograficas")
    class PropiedadesCriptograficas {

        @Test
        @DisplayName("dos cifrados del mismo texto son distintos: el IV no se reutiliza")
        void dos_cifrados_del_mismo_texto_son_distintos() {
            // Si esto fallara, AES-GCM dejaria de proteger: con IV repetido el mismo
            // valor produce el mismo ciphertext y el patron queda visible en la columna.
            String uno = converter.convertToDatabaseColumn("clave-dian-123");
            String otro = converter.convertToDatabaseColumn("clave-dian-123");

            assertThat(uno).isNotEqualTo(otro);
            assertThat(converter.convertToEntityAttribute(uno))
                    .isEqualTo(converter.convertToEntityAttribute(otro));
        }

        @Test
        @DisplayName("el valor persistido lleva prefijo de version, que es lo que permite rotar")
        void el_valor_persistido_lleva_prefijo_de_version() {
            String cifrado = converter.convertToDatabaseColumn("clave-dian-123");

            assertThat(cifrado).contains(":");
            String version = cifrado.substring(0, cifrado.indexOf(':'));
            assertThat(version).isNotBlank();
        }

        @Test
        @DisplayName("el cuerpo cifrado es base64 valido")
        void el_cuerpo_cifrado_es_base64_valido() {
            String cifrado = converter.convertToDatabaseColumn("clave-dian-123");
            String cuerpo = cifrado.substring(cifrado.indexOf(':') + 1);

            assertThat(Base64.getDecoder().decode(cuerpo)).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("datos corruptos")
    class DatosCorruptos {

        @Test
        @DisplayName("un ciphertext manipulado no se descifra en silencio")
        void un_ciphertext_manipulado_no_se_descifra_en_silencio() {
            // GCM autentica: alterar un byte tiene que fallar, no devolver basura.
            String cifrado = converter.convertToDatabaseColumn("clave-dian-123");
            String version = cifrado.substring(0, cifrado.indexOf(':') + 1);
            byte[] cuerpo = Base64.getDecoder().decode(cifrado.substring(cifrado.indexOf(':') + 1));
            cuerpo[cuerpo.length - 1] ^= 0x01;
            String manipulado = version + Base64.getEncoder().encodeToString(cuerpo);

            assertThatThrownBy(() -> converter.convertToEntityAttribute(manipulado))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("una version desconocida no se descifra con la clave activa")
        void una_version_desconocida_no_se_descifra() {
            String cifrado = converter.convertToDatabaseColumn("clave-dian-123");
            String manipulado = "vX" + cifrado.substring(cifrado.indexOf(':'));

            assertThatThrownBy(() -> converter.convertToEntityAttribute(manipulado))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
