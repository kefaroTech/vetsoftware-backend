package com.vetsoftware.app.infrastructure.audit.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

@DisplayName("AuditChainHash")
class AuditChainHashTest {

    @Nested
    @DisplayName("payloadHash")
    class PayloadHash {

        @Test
        @DisplayName("exige un payload no nulo")
        void exige_un_payload_no_nulo() {
            assertThatThrownBy(() -> AuditChainHash.payloadHash(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("payload es obligatorio");
        }

        @Test
        @DisplayName("produce un hexadecimal SHA-256 en minuscula de 64 caracteres")
        void produce_un_hexadecimal_sha256_en_minuscula() {
            String hash = AuditChainHash.payloadHash("{\"eventId\":\"e1\"}");

            assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("es determinista: el mismo payload siempre produce el mismo hash")
        void es_determinista() {
            String first = AuditChainHash.payloadHash("mismo-payload");
            String second = AuditChainHash.payloadHash("mismo-payload");

            assertThat(first).isEqualTo(second);
        }

        @Test
        @DisplayName("payloads distintos producen hashes distintos")
        void payloads_distintos_producen_hashes_distintos() {
            String first = AuditChainHash.payloadHash("payload-a");
            String second = AuditChainHash.payloadHash("payload-b");

            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("si el JDK no ofreciera SHA-256, se traduce en IllegalStateException")
        void si_el_jdk_no_ofreciera_sha256_se_traduce_en_illegal_state_exception() {
            // SHA-256 es obligatorio en toda implementación de la JVM, así que esta rama
            // de defensa nunca se dispara en producción; se fuerza aquí mockeando el
            // método estático para probar la traducción de la excepción.
            try (MockedStatic<MessageDigest> digest = mockStatic(MessageDigest.class)) {
                digest.when(() -> MessageDigest.getInstance("SHA-256"))
                        .thenThrow(new NoSuchAlgorithmException("sin proveedor"));

                assertThatThrownBy(() -> AuditChainHash.payloadHash("cualquiera"))
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("SHA-256 no disponible");
            }
        }
    }

    @Nested
    @DisplayName("chainHash")
    class ChainHash {

        private final String validHash = AuditChainHash.payloadHash("valor-valido");

        @Test
        @DisplayName("el eslabon es el hash del payload compuesto previousHash:sequence:payloadHash")
        void el_eslabon_es_el_hash_del_payload_compuesto() {
            String chainHash = AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH, 1, validHash);

            String expected = AuditChainHash
                    .payloadHash(AuditChainHash.GENESIS_HASH + ":" + 1 + ":" + validHash);
            assertThat(chainHash).isEqualTo(expected);
        }

        @Test
        @DisplayName("el hash genesis es un previousHash valido para el primer eslabon")
        void el_hash_genesis_es_un_previous_hash_valido() {
            String chainHash = AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH, 1, validHash);

            assertThat(chainHash).hasSize(64).matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("encadenar el mismo eslabon dos veces produce el mismo resultado")
        void encadenar_el_mismo_eslabon_dos_veces_produce_el_mismo_resultado() {
            String first = AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH, 5, validHash);
            String second = AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH, 5, validHash);

            assertThat(first).isEqualTo(second);
        }

        @Test
        @DisplayName("cambiar la posicion cambia el eslabon aunque el resto no cambie")
        void cambiar_la_posicion_cambia_el_eslabon() {
            String withSequenceOne = AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH, 1,
                    validHash);
            String withSequenceTwo = AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH, 2,
                    validHash);

            assertThat(withSequenceOne).isNotEqualTo(withSequenceTwo);
        }

        @ParameterizedTest
        @DisplayName("rechaza sequence menor que uno")
        @ValueSource(longs = {0L, -1L, -100L})
        void rechaza_sequence_menor_que_uno(long sequence) {
            assertThatThrownBy(() -> AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH, sequence,
                    validHash)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("sequence debe ser positiva");
        }

        @ParameterizedTest
        @DisplayName("rechaza un previousHash que no es hex de 64 caracteres")
        @NullSource
        @ValueSource(strings = {"", "corto",
                "no-es-hexadecimal-0000000000000000000000000000000000000000",
                "AAAA000000000000000000000000000000000000000000000000000000000A"})
        void rechaza_previous_hash_invalido(String previousHash) {
            assertThatThrownBy(() -> AuditChainHash.chainHash(previousHash, 1, validHash))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("previousHash");
        }

        @ParameterizedTest
        @DisplayName("rechaza un payloadHash que no es hex de 64 caracteres")
        @NullSource
        @ValueSource(strings = {"", "corto"})
        void rechaza_payload_hash_invalido(String payloadHash) {
            assertThatThrownBy(
                    () -> AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH, 1, payloadHash))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("payloadHash");
        }

        @Test
        @DisplayName("rechaza hexadecimal en mayuscula, aunque tenga la longitud correcta")
        void rechaza_hexadecimal_en_mayuscula() {
            String upperCase = validHash.toUpperCase(java.util.Locale.ROOT);

            assertThatThrownBy(() -> AuditChainHash.chainHash(upperCase, 1, validHash))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("hexadecimal minúsculo");
        }
    }
}
