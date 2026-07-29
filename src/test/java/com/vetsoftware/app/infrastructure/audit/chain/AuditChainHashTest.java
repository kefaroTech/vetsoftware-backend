package com.vetsoftware.app.infrastructure.audit.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AuditChainHashTest {

    /**
     * Vector conocido de SHA-256. Fija el formato de salida: hexadecimal minúsculo de 64
     * caracteres, que es lo que produce {@code SHA2(x, 256)} de MySQL y lo que usa el relleno de la
     * migración 215. Si alguien cambiara el algoritmo o pasara a mayúsculas, las filas rellenadas
     * dejarían de verificar.
     */
    @Test
    void el_hash_del_payload_es_sha256_hex_minusculo() {
        assertThat(AuditChainHash.payloadHash("abc")).isEqualTo(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void el_hash_del_payload_respeta_los_bytes_utf8() {
        // Si la implementación usara la codificación por defecto de la plataforma, este hash
        // cambiaría entre entornos y la verificación fallaría al mover de máquina.
        assertThat(AuditChainHash.payloadHash("{\"nombre\":\"José Ñuño\"}")).isEqualTo(
                AuditChainHash.payloadHash(new String(
                        "{\"nombre\":\"José Ñuño\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void el_eslabon_depende_de_los_tres_componentes() {
        String payloadHash = AuditChainHash.payloadHash("evento");
        String otroPayloadHash = AuditChainHash.payloadHash("otro evento");
        String base = AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH, 1, payloadHash);

        // Cambiar la posición, el payload o el eslabón anterior debe dar un hash distinto.
        assertThat(AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH, 2, payloadHash))
                .isNotEqualTo(base);
        assertThat(AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH, 1, otroPayloadHash))
                .isNotEqualTo(base);
        assertThat(AuditChainHash.chainHash(payloadHash, 1, payloadHash)).isNotEqualTo(base);
    }

    @Test
    void el_eslabon_es_determinista() {
        String payloadHash = AuditChainHash.payloadHash("evento");
        assertThat(AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH, 7, payloadHash))
                .isEqualTo(AuditChainHash.chainHash(AuditChainHash.GENESIS_HASH, 7, payloadHash));
    }

    @Test
    void rechaza_hashes_con_formato_invalido() {
        String valido = AuditChainHash.payloadHash("evento");

        assertThatThrownBy(() -> AuditChainHash.chainHash("corto", 1, valido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("64 caracteres");
        assertThatThrownBy(() -> AuditChainHash.chainHash(valido.toUpperCase(), 1, valido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minúsculo");
        assertThatThrownBy(() -> AuditChainHash.chainHash(valido, 0, valido))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positiva");
    }

    @Test
    void el_genesis_tiene_el_formato_de_un_hash() {
        assertThat(AuditChainHash.GENESIS_HASH).hasSize(64).containsOnlyDigits();
        // Debe ser usable como previous_hash del primer eslabón.
        assertThat(AuditChainHash.chainHash(
                AuditChainHash.GENESIS_HASH, 1, AuditChainHash.payloadHash("x"))).hasSize(64);
    }
}
