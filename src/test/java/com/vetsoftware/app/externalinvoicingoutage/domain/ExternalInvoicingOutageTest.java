package com.vetsoftware.app.externalinvoicingoutage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.externalinvoicingoutage.testsupport.ExternalInvoicingOutageMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ExternalInvoicingOutage — invariantes y ciclo de vida del agregado")
class ExternalInvoicingOutageTest {

    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = ExternalInvoicingOutageMother.OUTAGE_ID;
        private LocalDateTime startedAt = ExternalInvoicingOutageMother.STARTED_AT;
        private LocalDateTime endedAt;
        private CauseParty causeParty = ExternalInvoicingOutageMother.CAUSE_PARTY;
        private String summary = ExternalInvoicingOutageMother.SUMMARY;
        private int affectedCompanyCount = ExternalInvoicingOutageMother.AFFECTED_COMPANY_COUNT;
        private LocalDateTime notifiedCompaniesAt;
        private String externalIncidentRef = ExternalInvoicingOutageMother.EXTERNAL_INCIDENT_REF;
        private LocalDateTime createdDate = ExternalInvoicingOutageMother.CREATED_DATE;

        private Builder startedAt(LocalDateTime v) {
            this.startedAt = v;
            return this;
        }

        private Builder endedAt(LocalDateTime v) {
            this.endedAt = v;
            return this;
        }

        private Builder causeParty(CauseParty v) {
            this.causeParty = v;
            return this;
        }

        private Builder summary(String v) {
            this.summary = v;
            return this;
        }

        private Builder affectedCompanyCount(int v) {
            this.affectedCompanyCount = v;
            return this;
        }

        private Builder notifiedCompaniesAt(LocalDateTime v) {
            this.notifiedCompaniesAt = v;
            return this;
        }

        private Builder externalIncidentRef(String v) {
            this.externalIncidentRef = v;
            return this;
        }

        private Builder createdDate(LocalDateTime v) {
            this.createdDate = v;
            return this;
        }

        private ExternalInvoicingOutage build() {
            return new ExternalInvoicingOutage(id, startedAt, endedAt, causeParty, summary,
                    affectedCompanyCount, notifiedCompaniesAt, externalIncidentRef, createdDate,
                    ExternalInvoicingOutageMother.VERSION);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            ExternalInvoicingOutage caida = ExternalInvoicingOutageMother.cerrada();

            assertThat(caida.getId()).isEqualTo(ExternalInvoicingOutageMother.OUTAGE_ID);
            assertThat(caida.getStartedAt()).isEqualTo(ExternalInvoicingOutageMother.STARTED_AT);
            assertThat(caida.getEndedAt()).isEqualTo(ExternalInvoicingOutageMother.ENDED_AT);
            assertThat(caida.getCauseParty()).isEqualTo(ExternalInvoicingOutageMother.CAUSE_PARTY);
            assertThat(caida.getSummary()).isEqualTo(ExternalInvoicingOutageMother.SUMMARY);
            assertThat(caida.getAffectedCompanyCount())
                    .isEqualTo(ExternalInvoicingOutageMother.NOTIFIED_COMPANY_COUNT);
            assertThat(caida.getNotifiedCompaniesAt())
                    .isEqualTo(ExternalInvoicingOutageMother.NOTIFIED_COMPANIES_AT);
            assertThat(caida.getExternalIncidentRef())
                    .isEqualTo(ExternalInvoicingOutageMother.EXTERNAL_INCIDENT_REF);
            assertThat(caida.getCreatedDate())
                    .isEqualTo(ExternalInvoicingOutageMother.CREATED_DATE);
        }

        @Test
        @DisplayName("externalIncidentRef es opcional: nulo no revienta la construccion")
        void external_incident_ref_es_opcional() {
            ExternalInvoicingOutage caida = valido().externalIncidentRef(null).build();

            assertThat(caida.getExternalIncidentRef()).isNull();
        }
    }

    @Nested
    @DisplayName("validaciones basicas")
    class Validaciones {

        @Test
        @DisplayName("startedAt nulo revienta")
        void started_at_nulo_revienta() {
            assertThatThrownBy(() -> valido().startedAt(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("startedAt is required");
        }

        @Test
        @DisplayName("causeParty nulo revienta")
        void cause_party_nulo_revienta() {
            assertThatThrownBy(() -> valido().causeParty(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("causeParty is required");
        }

        @Test
        @DisplayName("summary en blanco revienta")
        void summary_en_blanco_revienta() {
            assertThatThrownBy(() -> valido().summary("   ").build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("summary is required");
        }

        @Test
        @DisplayName("summary por encima de 255 caracteres revienta")
        void summary_supera_255_caracteres_revienta() {
            assertThatThrownBy(() -> valido().summary("x".repeat(256)).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("summary must be 255 chars or less");
        }

        @Test
        @DisplayName("affectedCompanyCount negativo revienta")
        void affected_company_count_negativo_revienta() {
            assertThatThrownBy(() -> valido().affectedCompanyCount(-1).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("affectedCompanyCount must not be negative");
        }

        @Test
        @DisplayName("externalIncidentRef por encima de 100 caracteres revienta")
        void external_incident_ref_supera_100_caracteres_revienta() {
            assertThatThrownBy(() -> valido().externalIncidentRef("R".repeat(101)).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("externalIncidentRef must be 100 chars or less");
        }

        @Test
        @DisplayName("createdDate nulo revienta")
        void created_date_nulo_revienta() {
            assertThatThrownBy(() -> valido().createdDate(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("createdDate is required");
        }
    }

    @Nested
    @DisplayName("cierre — chk_eio_ended, limite estricto")
    class Cierre {

        @Test
        @DisplayName("endedAt igual a startedAt revienta: no midio nada")
        void ended_at_igual_a_started_at_revienta() {
            assertThatThrownBy(
                    () -> valido().endedAt(ExternalInvoicingOutageMother.STARTED_AT).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("endedAt must be after startedAt");
        }

        @Test
        @DisplayName("endedAt anterior a startedAt revienta")
        void ended_at_anterior_a_started_at_revienta() {
            assertThatThrownBy(() -> valido()
                    .endedAt(ExternalInvoicingOutageMother.STARTED_AT.minusHours(1)).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("endedAt must be after startedAt");
        }

        @Test
        @DisplayName("end() la primera vez pone la hora de fin y conserva el resto de la caida")
        void end_la_primera_vez_pone_la_hora_de_fin() {
            ExternalInvoicingOutage abierta = ExternalInvoicingOutageMother.abierta();

            ExternalInvoicingOutage cerrada = abierta.end(ExternalInvoicingOutageMother.ENDED_AT);

            assertThat(cerrada.getEndedAt()).isEqualTo(ExternalInvoicingOutageMother.ENDED_AT);
            assertThat(cerrada.isOpen()).isFalse();
            assertThat(cerrada.getId()).isEqualTo(abierta.getId());
            assertThat(cerrada.getStartedAt()).isEqualTo(abierta.getStartedAt());
        }

        @Test
        @DisplayName("cerrar dos veces revienta: la hora de vuelta no se reescribe")
        void cerrar_dos_veces_revienta() {
            ExternalInvoicingOutage cerrada = ExternalInvoicingOutageMother.cerrada();

            assertThatThrownBy(
                    () -> cerrada.end(ExternalInvoicingOutageMother.ENDED_AT.plusHours(2)))
                    .isInstanceOf(ExternalInvoicingOutageAlreadyEndedException.class)
                    .hasMessageContaining(
                            "External invoicing outage " + ExternalInvoicingOutageMother.OUTAGE_ID);
        }
    }

    @Nested
    @DisplayName("aviso a las clinicas — chk_eio_notified, limite NO estricto")
    class Aviso {

        @Test
        @DisplayName("notifiedCompaniesAt anterior a startedAt revienta")
        void notified_companies_at_anterior_a_started_at_revienta() {
            assertThatThrownBy(() -> valido()
                    .notifiedCompaniesAt(ExternalInvoicingOutageMother.STARTED_AT.minusMinutes(1))
                    .build()).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("notifiedCompaniesAt must not precede startedAt");
        }

        @Test
        @DisplayName("notifiedCompaniesAt igual a startedAt es legitimo: no es un limite estricto")
        void notified_companies_at_igual_a_started_at_es_legitimo() {
            ExternalInvoicingOutage caida = valido()
                    .notifiedCompaniesAt(ExternalInvoicingOutageMother.STARTED_AT).build();

            assertThat(caida.getNotifiedCompaniesAt())
                    .isEqualTo(ExternalInvoicingOutageMother.STARTED_AT);
        }

        @Test
        @DisplayName("notifyCompanies anota la marca y sobrescribe el contador con el corregido")
        void notify_companies_anota_la_marca_y_el_contador_corregido() {
            ExternalInvoicingOutage abierta = ExternalInvoicingOutageMother.abierta();

            ExternalInvoicingOutage notificada = abierta.notifyCompanies(
                    ExternalInvoicingOutageMother.NOTIFIED_COMPANIES_AT,
                    ExternalInvoicingOutageMother.NOTIFIED_COMPANY_COUNT);

            assertThat(notificada.getNotifiedCompaniesAt())
                    .isEqualTo(ExternalInvoicingOutageMother.NOTIFIED_COMPANIES_AT);
            assertThat(notificada.getAffectedCompanyCount())
                    .isEqualTo(ExternalInvoicingOutageMother.NOTIFIED_COMPANY_COUNT);
            assertThat(notificada.getId()).isEqualTo(abierta.getId());
        }

        @Test
        @DisplayName("es idempotente: un segundo aviso sobrescribe al primero con el valor nuevo")
        void es_idempotente_y_sobrescribe_al_primero() {
            ExternalInvoicingOutage primerAviso = ExternalInvoicingOutageMother.abierta()
                    .notifyCompanies(ExternalInvoicingOutageMother.NOTIFIED_COMPANIES_AT, 38);
            LocalDateTime segundaMarca = ExternalInvoicingOutageMother.NOTIFIED_COMPANIES_AT
                    .plusHours(3);

            ExternalInvoicingOutage segundoAviso = primerAviso.notifyCompanies(segundaMarca, 45);

            assertThat(segundoAviso.getNotifiedCompaniesAt()).isEqualTo(segundaMarca);
            assertThat(segundoAviso.getAffectedCompanyCount()).isEqualTo(45);
        }
    }

    @Nested
    @DisplayName("consultas")
    class Consultas {

        @Test
        @DisplayName("isOpen es verdadero mientras no tiene hora de fin")
        void is_open_true_mientras_no_tiene_hora_de_fin() {
            assertThat(ExternalInvoicingOutageMother.abierta().isOpen()).isTrue();
        }

        @Test
        @DisplayName("isOpen es falso una vez cerrada")
        void is_open_false_una_vez_cerrada() {
            assertThat(ExternalInvoicingOutageMother.cerrada().isOpen()).isFalse();
        }

        @Test
        @DisplayName("openOutageMarker devuelve el causante mientras esta abierta")
        void open_outage_marker_devuelve_el_causante_mientras_esta_abierta() {
            assertThat(ExternalInvoicingOutageMother.abierta().openOutageMarker())
                    .isEqualTo(ExternalInvoicingOutageMother.CAUSE_PARTY.name());
        }

        @Test
        @DisplayName("openOutageMarker es null una vez cerrada: uq_eio_open deja de restringir")
        void open_outage_marker_es_null_una_vez_cerrada() {
            assertThat(ExternalInvoicingOutageMother.cerrada().openOutageMarker()).isNull();
        }
    }
}
