package com.vetsoftware.app.electronicdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Identidad fiscal congelada del emisor. Si el perfil de la empresa cambia
 * despues, el documento ya emitido conserva esta copia: por eso la lista de
 * responsabilidades RUT tiene que quedar blindada frente a la lista que entrego
 * el llamador.
 */
@DisplayName("IssuerSnapshot — identidad congelada del emisor")
class IssuerSnapshotTest {

    private static IssuerSnapshot conDocumentoYRazonSocial(String documentId, String legalName) {
        return new IssuerSnapshot("NIT", documentId, "7", legalName, "RESPONSABLE", "vet@vet.co",
                List.of("O-13"));
    }

    @Nested
    @DisplayName("invariantes")
    class Invariantes {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        @DisplayName("rechaza un emisor sin NIT")
        void rechaza_un_emisor_sin_nit(String documentId) {
            assertThatThrownBy(() -> conDocumentoYRazonSocial(documentId, "Vet SAS"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("issuer documentId is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        @DisplayName("rechaza un emisor sin razon social")
        void rechaza_un_emisor_sin_razon_social(String legalName) {
            assertThatThrownBy(() -> conDocumentoYRazonSocial("900123456", legalName))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("issuer legalName is required");
        }

        @Test
        @DisplayName("conserva todos los campos tal como se congelaron")
        void conserva_todos_los_campos() {
            IssuerSnapshot issuer = conDocumentoYRazonSocial("900123456", "Vet SAS");

            assertThat(issuer.documentType()).isEqualTo("NIT");
            assertThat(issuer.documentId()).isEqualTo("900123456");
            assertThat(issuer.verificationDigit()).isEqualTo("7");
            assertThat(issuer.legalName()).isEqualTo("Vet SAS");
            assertThat(issuer.taxRegime()).isEqualTo("RESPONSABLE");
            assertThat(issuer.email()).isEqualTo("vet@vet.co");
            assertThat(issuer.responsibilities()).containsExactly("O-13");
        }
    }

    @Nested
    @DisplayName("responsabilidades del RUT")
    class Responsabilidades {

        @Test
        @DisplayName("una lista null se normaliza a lista vacia, no explota")
        void una_lista_null_se_normaliza_a_vacia() {
            IssuerSnapshot issuer = new IssuerSnapshot("NIT", "900123456", "7", "Vet SAS",
                    "RESPONSABLE", "vet@vet.co", null);

            assertThat(issuer.responsibilities()).isEmpty();
        }

        @Test
        @DisplayName("conserva el orden y las repeticiones de los codigos entregados")
        void conserva_el_orden_de_los_codigos() {
            IssuerSnapshot issuer = new IssuerSnapshot("NIT", "900123456", "7", "Vet SAS",
                    "RESPONSABLE", "vet@vet.co", List.of("O-15", "O-13", "R-99-PN"));

            assertThat(issuer.responsibilities()).containsExactly("O-15", "O-13", "R-99-PN");
        }

        @Test
        @DisplayName("mutar la lista original despues no cambia el snapshot ya congelado")
        void mutar_la_lista_original_no_cambia_el_snapshot() {
            List<String> original = new ArrayList<>(List.of("O-13"));
            IssuerSnapshot issuer = new IssuerSnapshot("NIT", "900123456", "7", "Vet SAS",
                    "RESPONSABLE", "vet@vet.co", original);

            original.add("O-47");

            assertThat(issuer.responsibilities()).containsExactly("O-13");
        }

        @Test
        @DisplayName("la lista expuesta es inmodificable desde fuera")
        void la_lista_expuesta_es_inmodificable() {
            IssuerSnapshot issuer = conDocumentoYRazonSocial("900123456", "Vet SAS");

            assertThatThrownBy(() -> issuer.responsibilities().add("O-47"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
