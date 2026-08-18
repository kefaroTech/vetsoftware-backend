package com.vetsoftware.app.owner.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("TaxRegime.defaultFor — inferencia del regimen de IVA")
class TaxRegimeTest {

    /**
     * Las cuatro combinaciones de la condicion {@code personType == JURIDICA ||
     * documentType == NIT}: cada una fuerza una rama distinta del operador OR.
     */
    static Stream<Arguments> combinaciones() {
        return Stream.of(
                arguments(PersonType.JURIDICA, OwnerDocumentType.CEDULA_CIUDADANIA,
                        TaxRegime.RESPONSABLE_IVA),
                arguments(PersonType.NATURAL, OwnerDocumentType.NIT, TaxRegime.RESPONSABLE_IVA),
                arguments(PersonType.JURIDICA, OwnerDocumentType.NIT, TaxRegime.RESPONSABLE_IVA),
                arguments(PersonType.NATURAL, OwnerDocumentType.CEDULA_CIUDADANIA,
                        TaxRegime.NO_RESPONSABLE_IVA),
                arguments(PersonType.NATURAL, OwnerDocumentType.CEDULA_EXTRANJERIA,
                        TaxRegime.NO_RESPONSABLE_IVA),
                arguments(PersonType.NATURAL, OwnerDocumentType.PASAPORTE,
                        TaxRegime.NO_RESPONSABLE_IVA),
                arguments(PersonType.NATURAL, OwnerDocumentType.PEP, TaxRegime.NO_RESPONSABLE_IVA));
    }

    @ParameterizedTest(name = "{0} x {1} -> {2}")
    @MethodSource("combinaciones")
    @DisplayName("juridica o NIT es responsable de IVA; el resto no")
    void juridica_o_nit_es_responsable_de_iva(PersonType personType, OwnerDocumentType documentType,
            TaxRegime esperado) {
        assertThat(TaxRegime.defaultFor(personType, documentType)).isEqualTo(esperado);
    }
}
