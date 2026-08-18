package com.vetsoftware.app.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
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

    /**
     * {@code loadKeys()} solo se ejecuta una vez, al cargar la clase (campo
     * {@code static final MATERIAL}), leyendo variables de entorno reales: no se
     * puede re-ejercitar con distintos escenarios de {@code DIAN_ENC_KEY} /
     * {@code DIAN_ENC_KEY_PREVIOUS} dentro de la misma JVM sin manipular el entorno
     * del proceso (fuera de alcance de un test unitario). En cambio
     * {@code resolveVersion} y {@code toKey} son funciones puras — no leen
     * {@code MATERIAL} ni el entorno — y sí se pueden ejercitar directamente por
     * reflexion, cazando las ramas de validacion que ese arranque unico deja sin
     * cubrir.
     */
    @Nested
    @DisplayName("metodos privados deterministas (resolveVersion / toKey)")
    class MetodosPrivadosDeterministas {

        @Test
        @DisplayName("usa la version configurada cuando viene informada")
        void usa_la_version_configurada_cuando_viene_informada() throws Exception {
            assertThat(invokeResolveVersion("v2", "v1")).isEqualTo("v2");
        }

        @Test
        @DisplayName("recorta espacios de la version configurada")
        void recorta_espacios_de_la_version_configurada() throws Exception {
            assertThat(invokeResolveVersion("  v3  ", "v1")).isEqualTo("v3");
        }

        @Test
        @DisplayName("usa el fallback si la version configurada es null")
        void usa_el_fallback_si_la_version_configurada_es_null() throws Exception {
            assertThat(invokeResolveVersion(null, "v1")).isEqualTo("v1");
        }

        @Test
        @DisplayName("usa el fallback si la version configurada esta en blanco")
        void usa_el_fallback_si_la_version_configurada_esta_en_blanco() throws Exception {
            assertThat(invokeResolveVersion("   ", "v1")).isEqualTo("v1");
        }

        @ParameterizedTest(name = "\"{0}\" contiene el separador de version")
        @ValueSource(strings = {"v1:extra", "a:b", ":"})
        @DisplayName("una version con el separador ':' no se acepta")
        void una_version_con_el_separador_no_se_acepta(String versionInvalida) {
            assertThatThrownBy(() -> invokeResolveVersion(versionInvalida, "v1"))
                    .isInstanceOf(InvocationTargetException.class).cause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no puede contener");
        }

        @Test
        @DisplayName("una clave que no decodifica a 32 bytes no se acepta")
        void una_clave_que_no_decodifica_a_32_bytes_no_se_acepta() {
            String claveCorta = Base64.getEncoder().encodeToString("demasiado-corta".getBytes());

            assertThatThrownBy(() -> invokeToKey(claveCorta))
                    .isInstanceOf(InvocationTargetException.class).cause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("must decode to 32 bytes");
        }

        @Test
        @DisplayName("una clave de 32 bytes en base64 produce una SecretKeySpec AES")
        void una_clave_de_32_bytes_produce_una_secret_key_spec_aes() throws Exception {
            String clave32Bytes = Base64.getEncoder().encodeToString(
                    "0123456789012345678901234567890123456789".substring(0, 32).getBytes());

            SecretKeySpec secretKeySpec = (SecretKeySpec) invokeToKey(clave32Bytes);

            assertThat(secretKeySpec.getAlgorithm()).isEqualTo("AES");
            assertThat(secretKeySpec.getEncoded()).hasSize(32);
        }

        private String invokeResolveVersion(String configured, String fallback) throws Exception {
            Method resolveVersion = EncryptedStringConverter.class
                    .getDeclaredMethod("resolveVersion", String.class, String.class);
            resolveVersion.setAccessible(true);
            return (String) resolveVersion.invoke(null, configured, fallback);
        }

        private Object invokeToKey(String base64) throws Exception {
            Method toKey = EncryptedStringConverter.class.getDeclaredMethod("toKey", String.class);
            toKey.setAccessible(true);
            return toKey.invoke(null, base64);
        }
    }
}
