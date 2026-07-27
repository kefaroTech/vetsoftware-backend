package com.vetsoftware.app.petshopcatalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PetshopCatalogRulesTest {
    @Test
    void barcodePreservesLeadingZerosAndTrimsOuterWhitespace() {
        assertThat(PetshopCatalogRules.barcodes(List.of(" 001234 ")))
            .containsExactly("001234");
    }

    @Test
    void barcodeRejectsDuplicatesAfterNormalization() {
        assertThatThrownBy(() ->
            PetshopCatalogRules.barcodes(List.of("ABC", " ABC ")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("repeated");
    }

    @Test
    void barcodeRejectsControlCharacters() {
        assertThatThrownBy(() ->
            PetshopCatalogRules.barcodes(List.of("ABC\n123")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("control");
    }

    @Test
    void barcodeRejectsInternalWhitespace() {
        assertThatThrownBy(() ->
            PetshopCatalogRules.barcodes(List.of("ABC 123")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("whitespace");
    }

    @Test
    void defaultPresentationRequiresOneBaseUnit() {
        PetshopCatalogRules.defaultFactor(true, 1);

        assertThatThrownBy(() -> PetshopCatalogRules.defaultFactor(true, 6))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("conversionFactor 1");
    }

    @Test
    void expectedVersionRejectsStaleClients() {
        assertThatThrownBy(() -> PetshopCatalogRules.expectedVersion(3L, 4L))
            .isInstanceOf(PetshopCatalogConflictException.class)
            .extracting("code")
            .isEqualTo("CONCURRENT_MODIFICATION");
    }

    @Test
    void displayOrderMayStartAtZeroButCannotBeNegative() {
        assertThat(PetshopCatalogRules.nonNegative(0, "displayOrder")).isZero();
        assertThatThrownBy(() -> PetshopCatalogRules.nonNegative(-1, "displayOrder"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
