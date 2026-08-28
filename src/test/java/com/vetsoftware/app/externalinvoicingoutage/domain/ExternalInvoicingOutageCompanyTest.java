package com.vetsoftware.app.externalinvoicingoutage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.externalinvoicingoutage.testsupport.ExternalInvoicingOutageMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

@DisplayName("ExternalInvoicingOutageCompany — invariantes del reparto por clinica")
class ExternalInvoicingOutageCompanyTest {

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            ExternalInvoicingOutageCompany afectada = ExternalInvoicingOutageMother.afectada();

            assertThat(afectada.getId()).isEqualTo(ExternalInvoicingOutageMother.AFFECTED_ID);
            assertThat(afectada.getOutageId()).isEqualTo(ExternalInvoicingOutageMother.OUTAGE_ID);
            assertThat(afectada.getCompanyId()).isEqualTo(ExternalInvoicingOutageMother.COMPANY_ID);
            assertThat(afectada.getFailedDocumentCount())
                    .isEqualTo(ExternalInvoicingOutageMother.FAILED_DOCUMENT_COUNT);
            assertThat(afectada.getResolvedBy())
                    .isEqualTo(ExternalInvoicingOutageMother.RESOLVED_BY);
        }

        @Test
        @DisplayName("cero documentos fallidos es legitimo: la clinica pudo no intentar emitir nada")
        void cero_documentos_fallidos_es_legitimo() {
            ExternalInvoicingOutageCompany afectada = new ExternalInvoicingOutageCompany(
                    ExternalInvoicingOutageMother.AFFECTED_ID,
                    ExternalInvoicingOutageMother.OUTAGE_ID,
                    ExternalInvoicingOutageMother.COMPANY_ID, 0,
                    ExternalInvoicingOutageMother.RESOLVED_BY);

            assertThat(afectada.getFailedDocumentCount()).isZero();
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("outageId nulo revienta")
        void outage_id_nulo_revienta() {
            assertThatThrownBy(() -> new ExternalInvoicingOutageCompany(
                    ExternalInvoicingOutageMother.AFFECTED_ID, null,
                    ExternalInvoicingOutageMother.COMPANY_ID,
                    ExternalInvoicingOutageMother.FAILED_DOCUMENT_COUNT,
                    ExternalInvoicingOutageMother.RESOLVED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("outageId is required");
        }

        @Test
        @DisplayName("companyId nulo revienta")
        void company_id_nulo_revienta() {
            assertThatThrownBy(() -> new ExternalInvoicingOutageCompany(
                    ExternalInvoicingOutageMother.AFFECTED_ID,
                    ExternalInvoicingOutageMother.OUTAGE_ID, null,
                    ExternalInvoicingOutageMother.FAILED_DOCUMENT_COUNT,
                    ExternalInvoicingOutageMother.RESOLVED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId is required");
        }

        @Test
        @DisplayName("failedDocumentCount negativo revienta")
        void failed_document_count_negativo_revienta() {
            assertThatThrownBy(() -> new ExternalInvoicingOutageCompany(
                    ExternalInvoicingOutageMother.AFFECTED_ID,
                    ExternalInvoicingOutageMother.OUTAGE_ID,
                    ExternalInvoicingOutageMother.COMPANY_ID, -1,
                    ExternalInvoicingOutageMother.RESOLVED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("failedDocumentCount must not be negative");
        }

        @Test
        @DisplayName("resolvedBy nulo revienta")
        void resolved_by_nulo_revienta() {
            assertThatThrownBy(() -> new ExternalInvoicingOutageCompany(
                    ExternalInvoicingOutageMother.AFFECTED_ID,
                    ExternalInvoicingOutageMother.OUTAGE_ID,
                    ExternalInvoicingOutageMother.COMPANY_ID,
                    ExternalInvoicingOutageMother.FAILED_DOCUMENT_COUNT, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("resolvedBy is required");
        }
    }

    @Nested
    @DisplayName("alta")
    class Alta {

        @Test
        @DisplayName("register nace sin id: lo genera la base")
        void register_nace_sin_id() {
            ExternalInvoicingOutageCompany afectada = ExternalInvoicingOutageCompany.register(
                    ExternalInvoicingOutageMother.OUTAGE_ID,
                    ExternalInvoicingOutageMother.COMPANY_ID,
                    ExternalInvoicingOutageMother.FAILED_DOCUMENT_COUNT,
                    ExternalInvoicingOutageMother.RESOLVED_BY);

            assertThat(afectada.getId()).isNull();
            assertThat(afectada.getOutageId()).isEqualTo(ExternalInvoicingOutageMother.OUTAGE_ID);
        }
    }

    @Nested
    @DisplayName("numeracion de contingencia — el hecho que hay que poder demostrar")
    class NumeracionDeContingencia {

        @Test
        @DisplayName("usedContingencyNumbering es verdadero cuando resolvedBy es CONTINGENCY_NUMBERING")
        void es_verdadero_cuando_resolved_by_es_contingencia() {
            ExternalInvoicingOutageCompany afectada = ExternalInvoicingOutageMother.afectada();

            assertThat(afectada.usedContingencyNumbering()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = OutageResolution.class, names = "CONTINGENCY_NUMBERING", mode = Mode.EXCLUDE)
        @DisplayName("usedContingencyNumbering es falso para cualquier otra resolucion")
        void es_falso_para_cualquier_otra_resolucion(OutageResolution resolucion) {
            ExternalInvoicingOutageCompany afectada = new ExternalInvoicingOutageCompany(
                    ExternalInvoicingOutageMother.AFFECTED_ID,
                    ExternalInvoicingOutageMother.OUTAGE_ID,
                    ExternalInvoicingOutageMother.COMPANY_ID,
                    ExternalInvoicingOutageMother.FAILED_DOCUMENT_COUNT, resolucion);

            assertThat(afectada.usedContingencyNumbering()).isFalse();
        }
    }
}
