package com.vetsoftware.app.electronicdocument.infrastructure.representation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ZxingQrGenerator — genera el QR como PNG en base64")
class ZxingQrGeneratorTest {

    private final ZxingQrGenerator generator = new ZxingQrGenerator();

    @Test
    @DisplayName("un contenido valido produce un PNG base64 no vacio y decodificable")
    void contenido_valido_produce_png_base64_decodificable() {
        String base64 = generator.generatePngBase64("https://catalogo-vpfe.dian.gov.co/CUFE-1");

        assertThat(base64).isNotBlank();
        byte[] png = Base64.getDecoder().decode(base64);
        assertThat(png.length).isGreaterThan(0);
        // Firma PNG: 0x89 'P' 'N' 'G'
        assertThat(png[1]).isEqualTo((byte) 'P');
        assertThat(png[2]).isEqualTo((byte) 'N');
        assertThat(png[3]).isEqualTo((byte) 'G');
    }

    @Test
    @DisplayName("contenidos distintos producen PNG distintos")
    void contenidos_distintos_producen_png_distintos() {
        String a = generator.generatePngBase64("CUFE-A");
        String b = generator.generatePngBase64("CUFE-B");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("un contenido que excede la capacidad del QR envuelve la falla de zxing")
    void contenido_que_excede_la_capacidad_lanza_illegal_state() {
        String demasiadoLargo = "a".repeat(8000);

        assertThatThrownBy(() -> generator.generatePngBase64(demasiadoLargo))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to generate QR code");
    }
}
