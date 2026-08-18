package com.vetsoftware.app.petshopcatalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PetshopCatalogRulesTest {
    @Test
    void barcodePreservesLeadingZerosAndTrimsOuterWhitespace() {
        assertThat(PetshopCatalogRules.barcodes(List.of(" 001234 "))).containsExactly("001234");
    }

    @Test
    void barcodeRejectsDuplicatesAfterNormalization() {
        assertThatThrownBy(() -> PetshopCatalogRules.barcodes(List.of("ABC", " ABC ")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("repeated");
    }

    @Test
    void barcodeRejectsControlCharacters() {
        assertThatThrownBy(() -> PetshopCatalogRules.barcodes(List.of("ABC\n123")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("control");
    }

    @Test
    void barcodeRejectsInternalWhitespace() {
        assertThatThrownBy(() -> PetshopCatalogRules.barcodes(List.of("ABC 123")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("whitespace");
    }

    @Test
    void barcodesAcceptsNullListAsEmpty() {
        assertThat(PetshopCatalogRules.barcodes(null)).isEmpty();
    }

    @Test
    void defaultPresentationRequiresOneBaseUnit() {
        PetshopCatalogRules.defaultFactor(true, 1);

        assertThatThrownBy(() -> PetshopCatalogRules.defaultFactor(true, 6))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conversionFactor 1");
    }

    @Test
    void defaultFactorAllowsAnyFactorWhenNotDefault() {
        PetshopCatalogRules.defaultFactor(false, 12);
    }

    @Test
    void expectedVersionRejectsStaleClients() {
        assertThatThrownBy(() -> PetshopCatalogRules.expectedVersion(3L, 4L))
                .isInstanceOf(PetshopCatalogConflictException.class).extracting("code")
                .isEqualTo("CONCURRENT_MODIFICATION");
    }

    @Test
    void expectedVersionRequiresANonNullValue() {
        assertThatThrownBy(() -> PetshopCatalogRules.expectedVersion(null, 4L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expectedVersion is required");
    }

    @Test
    void displayOrderMayStartAtZeroButCannotBeNegative() {
        assertThat(PetshopCatalogRules.nonNegative(0, "displayOrder")).isZero();
        assertThatThrownBy(() -> PetshopCatalogRules.nonNegative(-1, "displayOrder"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void displayOrderRejectsNullValue() {
        assertThatThrownBy(() -> PetshopCatalogRules.nonNegative(null, "displayOrder"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("displayOrder must be zero or positive");
    }

    @Nested
    @DisplayName("text — longitud maxima")
    class Text {

        @Test
        @DisplayName("un texto nulo se rechaza")
        void un_texto_nulo_se_rechaza() {
            assertThatThrownBy(() -> PetshopCatalogRules.text(null, "name", 120))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("un texto en blanco se rechaza")
        void un_texto_en_blanco_se_rechaza() {
            assertThatThrownBy(() -> PetshopCatalogRules.text("   ", "name", 120))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("name is required");
        }

        @Test
        @DisplayName("un texto mas largo que el maximo permitido se rechaza")
        void un_texto_mas_largo_que_el_maximo_se_rechaza() {
            assertThatThrownBy(() -> PetshopCatalogRules.text("12345", "code", 4))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("exceeds 4 characters");
        }

        @Test
        @DisplayName("un texto dentro del limite se recorta y se conserva")
        void un_texto_dentro_del_limite_se_conserva() {
            assertThat(PetshopCatalogRules.text("  Unidad  ", "name", 120)).isEqualTo("Unidad");
        }
    }

    @Nested
    @DisplayName("price — cero o positivo")
    class Price {

        @Test
        @DisplayName("un precio nulo se rechaza")
        void un_precio_nulo_se_rechaza() {
            assertThatThrownBy(() -> PetshopCatalogRules.price(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("salePrice must be zero or positive");
        }

        @Test
        @DisplayName("un precio negativo se rechaza")
        void un_precio_negativo_se_rechaza() {
            assertThatThrownBy(() -> PetshopCatalogRules.price(new BigDecimal("-0.01")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("salePrice must be zero or positive");
        }

        @Test
        @DisplayName("cero y un precio positivo se aceptan tal cual")
        void cero_y_un_precio_positivo_se_aceptan() {
            assertThat(PetshopCatalogRules.price(BigDecimal.ZERO)).isEqualByComparingTo("0");
            assertThat(PetshopCatalogRules.price(new BigDecimal("3500")))
                    .isEqualByComparingTo("3500");
        }
    }

    @Nested
    @DisplayName("positive — mayor que cero")
    class Positive {

        @ParameterizedTest
        @NullSource
        @ValueSource(ints = {0, -1})
        @DisplayName("nulo, cero o negativo se rechazan")
        void nulo_cero_o_negativo_se_rechazan(Integer valor) {
            assertThatThrownBy(() -> PetshopCatalogRules.positive(valor, "conversionFactor"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("conversionFactor must be positive");
        }

        @Test
        @DisplayName("un valor mayor que cero se devuelve tal cual")
        void un_valor_mayor_que_cero_se_devuelve() {
            assertThat(PetshopCatalogRules.positive(12, "conversionFactor")).isEqualTo(12);
        }
    }
}
