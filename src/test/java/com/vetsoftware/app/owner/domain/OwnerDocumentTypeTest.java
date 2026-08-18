package com.vetsoftware.app.owner.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("OwnerDocumentType.dianCode — mapeo al codigo DIAN del tipo de documento")
class OwnerDocumentTypeTest {

    static Stream<Arguments> codigosDian() {
        return Stream.of(arguments(OwnerDocumentType.CEDULA_CIUDADANIA, 13),
                arguments(OwnerDocumentType.NIT, 31),
                arguments(OwnerDocumentType.CEDULA_EXTRANJERIA, 22),
                arguments(OwnerDocumentType.PASAPORTE, 41), arguments(OwnerDocumentType.PEP, 47));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("codigosDian")
    @DisplayName("cada tipo de documento expone su codigo DIAN")
    void cada_tipo_expone_su_codigo_dian(OwnerDocumentType documentType, int codigoEsperado) {
        assertThat(documentType.dianCode()).isEqualTo(codigoEsperado);
    }
}
