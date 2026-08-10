package com.vetsoftware.app.electronicdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Identidad fiscal congelada del adquiriente. Es lo que viaja a la DIAN como
 * receptor del documento, asi que sus dos campos obligatorios (documento y
 * nombre) no admiten vacio: un documento sin adquiriente identificable es un
 * rechazo fiscal.
 */
@DisplayName("CustomerSnapshot — identidad congelada del adquiriente")
class CustomerSnapshotTest {

    private static CustomerSnapshot conDocumentoYNombre(String documentId, String name) {
        return new CustomerSnapshot("CEDULA_CIUDADANIA", documentId, "3", "NATURAL", "Ana M Perez",
                name, "ana@correo.co", "05001", TaxRegime.RESPONSABLE_IVA,
                FiscalResponsibility.GRAN_CONTRIBUYENTE);
    }

    @Nested
    @DisplayName("invariantes")
    class Invariantes {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        @DisplayName("rechaza un adquiriente sin numero de documento")
        void rechaza_un_adquiriente_sin_numero_de_documento(String documentId) {
            assertThatThrownBy(() -> conDocumentoYNombre(documentId, "Ana Perez"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("customer documentId is required");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        @DisplayName("rechaza un adquiriente sin nombre")
        void rechaza_un_adquiriente_sin_nombre(String name) {
            assertThatThrownBy(() -> conDocumentoYNombre("1020304050", name))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("customer name is required");
        }

        @Test
        @DisplayName("acepta un adquiriente con documento y nombre, aunque el resto sea null")
        void acepta_un_adquiriente_minimo() {
            assertThatCode(() -> new CustomerSnapshot(null, "1020304050", null, null, null,
                    "Ana Perez", null, null, null, null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("conserva todos los campos tal como se congelaron")
        void conserva_todos_los_campos() {
            CustomerSnapshot snapshot = conDocumentoYNombre("1020304050", "Ana Perez");

            assertThat(snapshot.documentType()).isEqualTo("CEDULA_CIUDADANIA");
            assertThat(snapshot.documentId()).isEqualTo("1020304050");
            assertThat(snapshot.verificationDigit()).isEqualTo("3");
            assertThat(snapshot.personType()).isEqualTo("NATURAL");
            assertThat(snapshot.legalName()).isEqualTo("Ana M Perez");
            assertThat(snapshot.name()).isEqualTo("Ana Perez");
            assertThat(snapshot.email()).isEqualTo("ana@correo.co");
            assertThat(snapshot.cityDaneCode()).isEqualTo("05001");
            assertThat(snapshot.taxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
            assertThat(snapshot.fiscalResponsibility())
                    .isEqualTo(FiscalResponsibility.GRAN_CONTRIBUYENTE);
        }
    }

    @Nested
    @DisplayName("consumidor final")
    class ConsumidorFinal {

        @Test
        @DisplayName("usa el NIT generico de la DIAN para el adquiriente no identificado")
        void usa_el_nit_generico_de_la_dian() {
            assertThat(CustomerSnapshot.finalConsumer().documentId())
                    .isEqualTo(CustomerSnapshot.FINAL_CONSUMER_DOCUMENT).isEqualTo("222222222222");
        }

        @Test
        @DisplayName("nunca es responsable de IVA ni tiene responsabilidad fiscal especial")
        void nunca_es_responsable_de_iva() {
            CustomerSnapshot finalConsumer = CustomerSnapshot.finalConsumer();

            assertThat(finalConsumer.taxRegime()).isEqualTo(TaxRegime.NO_RESPONSABLE_IVA);
            assertThat(finalConsumer.fiscalResponsibility())
                    .isEqualTo(FiscalResponsibility.NO_APLICA);
        }

        @Test
        @DisplayName("se llama 'Consumidor final' y no arrastra datos personales")
        void no_arrastra_datos_personales() {
            CustomerSnapshot finalConsumer = CustomerSnapshot.finalConsumer();

            assertThat(finalConsumer.name()).isEqualTo("Consumidor final");
            assertThat(finalConsumer.legalName()).isNull();
            assertThat(finalConsumer.email()).isNull();
            assertThat(finalConsumer.verificationDigit()).isNull();
            assertThat(finalConsumer.cityDaneCode()).isNull();
        }

        @Test
        @DisplayName("es persona natural con cedula, igual que el snapshot normal")
        void es_persona_natural_con_cedula() {
            assertThat(CustomerSnapshot.finalConsumer().personType()).isEqualTo("NATURAL");
            assertThat(CustomerSnapshot.finalConsumer().documentType())
                    .isEqualTo("CEDULA_CIUDADANIA");
        }

        @Test
        @DisplayName("dos consumidores finales son iguales: es un valor, no una entidad")
        void dos_consumidores_finales_son_iguales() {
            assertThat(CustomerSnapshot.finalConsumer())
                    .isEqualTo(CustomerSnapshot.finalConsumer());
        }
    }
}
