package com.vetsoftware.app.infrastructure.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class PdfPropertiesTest {

    @Nested
    @DisplayName("construcción válida")
    class Construccion {

        @Test
        @DisplayName("acepta valores válidos y los expone tal cual")
        void acepta_valores_validos_y_los_expone_tal_cual() {
            PdfProperties properties = new PdfProperties(2, Duration.ofSeconds(30),
                    DataSize.ofMegabytes(5), DataSize.ofMegabytes(25));

            assertThat(properties.maxConcurrentRenders()).isEqualTo(2);
            assertThat(properties.acquireTimeout()).isEqualTo(Duration.ofSeconds(30));
            assertThat(properties.maxHtmlSize()).isEqualTo(DataSize.ofMegabytes(5));
            assertThat(properties.maxPdfSize()).isEqualTo(DataSize.ofMegabytes(25));
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("rechaza un tiempo de espera de adquisición en cero")
        void rechaza_un_acquire_timeout_en_cero() {
            assertThatThrownBy(() -> new PdfProperties(1, Duration.ZERO, DataSize.ofMegabytes(5),
                    DataSize.ofMegabytes(25))).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("acquireTimeout");
        }

        @Test
        @DisplayName("rechaza un tiempo de espera de adquisición negativo")
        void rechaza_un_acquire_timeout_negativo() {
            assertThatThrownBy(() -> new PdfProperties(1, Duration.ofSeconds(-1),
                    DataSize.ofMegabytes(5), DataSize.ofMegabytes(25)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("acquireTimeout");
        }

        @Test
        @DisplayName("rechaza un tamaño máximo de HTML no positivo")
        void rechaza_un_tamano_maximo_de_html_no_positivo() {
            assertThatThrownBy(() -> new PdfProperties(1, Duration.ofSeconds(30),
                    DataSize.ofBytes(0), DataSize.ofMegabytes(25)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxHtmlSize");
        }

        @Test
        @DisplayName("rechaza un tamaño máximo de PDF no positivo")
        void rechaza_un_tamano_maximo_de_pdf_no_positivo() {
            assertThatThrownBy(() -> new PdfProperties(1, Duration.ofSeconds(30),
                    DataSize.ofMegabytes(5), DataSize.ofBytes(-1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxPdfSize");
        }

        @Test
        @DisplayName("rechaza un tamaño que desborda el límite representable en un int")
        void rechaza_un_tamano_que_desborda_el_limite_representable() {
            assertThatThrownBy(() -> new PdfProperties(1, Duration.ofSeconds(30),
                    DataSize.ofBytes((long) Integer.MAX_VALUE + 1), DataSize.ofMegabytes(25)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("maxHtmlSize");
        }
    }
}
