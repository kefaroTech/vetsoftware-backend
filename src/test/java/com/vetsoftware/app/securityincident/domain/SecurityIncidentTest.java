package com.vetsoftware.app.securityincident.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.securityincident.testsupport.SecurityIncidentMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SecurityIncident — invariantes y ciclo de vida del agregado")
class SecurityIncidentTest {

    /**
     * Constructor de fixtures con un campo variable por caso, en el estado "recien
     * registrado" (sin reportar, sin cerrar). Evita repetir diecisiete argumentos
     * por escenario invalido.
     */
    private static Builder valido() {
        return new Builder();
    }

    private static final class Builder {
        private Long id = SecurityIncidentMother.INCIDENT_ID;
        private LocalDateTime detectedAt = SecurityIncidentMother.DETECTED_AT;
        private LocalDateTime occurredAt = SecurityIncidentMother.OCCURRED_AT;
        private LocalDateTime escalatedAt = SecurityIncidentMother.ESCALATED_AT;
        private SecurityIncidentKind kind = SecurityIncidentMother.KIND;
        private IncidentSeverity severity = SecurityIncidentMother.SEVERITY;
        private String summary = SecurityIncidentMother.SUMMARY;
        private int affectedSubjectCount = SecurityIncidentMother.AFFECTED_SUBJECT_COUNT;
        private LocalDateTime deadlineAt = SecurityIncidentMother.DEADLINE_AT;
        private LocalDateTime createdDate = SecurityIncidentMother.CREATED_DATE;

        private Builder detectedAt(LocalDateTime v) {
            this.detectedAt = v;
            return this;
        }

        private Builder occurredAt(LocalDateTime v) {
            this.occurredAt = v;
            return this;
        }

        private Builder escalatedAt(LocalDateTime v) {
            this.escalatedAt = v;
            return this;
        }

        private Builder kind(SecurityIncidentKind v) {
            this.kind = v;
            return this;
        }

        private Builder severity(IncidentSeverity v) {
            this.severity = v;
            return this;
        }

        private Builder summary(String v) {
            this.summary = v;
            return this;
        }

        private Builder affectedSubjectCount(int v) {
            this.affectedSubjectCount = v;
            return this;
        }

        private Builder deadlineAt(LocalDateTime v) {
            this.deadlineAt = v;
            return this;
        }

        private Builder createdDate(LocalDateTime v) {
            this.createdDate = v;
            return this;
        }

        private SecurityIncident build() {
            return new SecurityIncident(id, detectedAt, occurredAt, escalatedAt, kind, severity,
                    summary, affectedSubjectCount, deadlineAt, null, null, null, null, null, null,
                    createdDate, SecurityIncidentMother.VERSION);
        }
    }

    @Nested
    @DisplayName("construccion")
    class Construccion {

        @Test
        @DisplayName("el constructor conserva cada campo en su sitio")
        void el_constructor_conserva_cada_campo_en_su_sitio() {
            SecurityIncident incidente = SecurityIncidentMother.cerrado();

            assertThat(incidente.getId()).isEqualTo(SecurityIncidentMother.INCIDENT_ID);
            assertThat(incidente.getDetectedAt()).isEqualTo(SecurityIncidentMother.DETECTED_AT);
            assertThat(incidente.getOccurredAt()).isEqualTo(SecurityIncidentMother.OCCURRED_AT);
            assertThat(incidente.getEscalatedAt()).isEqualTo(SecurityIncidentMother.ESCALATED_AT);
            assertThat(incidente.getKind()).isEqualTo(SecurityIncidentMother.KIND);
            assertThat(incidente.getSeverity()).isEqualTo(SecurityIncidentMother.SEVERITY);
            assertThat(incidente.getSummary()).isEqualTo(SecurityIncidentMother.SUMMARY);
            assertThat(incidente.getAffectedSubjectCount())
                    .isEqualTo(SecurityIncidentMother.AFFECTED_SUBJECT_COUNT);
            assertThat(incidente.getDeadlineAt()).isEqualTo(SecurityIncidentMother.DEADLINE_AT);
            assertThat(incidente.getReportedToAuthorityAt())
                    .isEqualTo(SecurityIncidentMother.REPORTED_AT);
            assertThat(incidente.getReportReference())
                    .isEqualTo(SecurityIncidentMother.REPORT_REFERENCE);
            assertThat(incidente.getNotifiedSubjectsAt())
                    .isEqualTo(SecurityIncidentMother.NOTIFIED_SUBJECTS_AT);
            assertThat(incidente.getContainment()).isEqualTo(SecurityIncidentMother.CONTAINMENT);
            assertThat(incidente.getRootCause()).isEqualTo(SecurityIncidentMother.ROOT_CAUSE);
            assertThat(incidente.getClosedAt()).isEqualTo(SecurityIncidentMother.CLOSED_AT);
            assertThat(incidente.getCreatedDate()).isEqualTo(SecurityIncidentMother.CREATED_DATE);
        }

        @Test
        @DisplayName("occurredAt es opcional: nulo no revienta la construccion")
        void occurred_at_es_opcional() {
            SecurityIncident incidente = valido().occurredAt(null).build();

            assertThat(incidente.getOccurredAt()).isNull();
        }
    }

    @Nested
    @DisplayName("validaciones basicas")
    class Validaciones {

        @Test
        @DisplayName("detectedAt nulo revienta")
        void detected_at_nulo_revienta() {
            assertThatThrownBy(() -> valido().detectedAt(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("detectedAt is required");
        }

        @Test
        @DisplayName("kind nulo revienta")
        void kind_nulo_revienta() {
            assertThatThrownBy(() -> valido().kind(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("kind is required");
        }

        @Test
        @DisplayName("severity nula revienta")
        void severity_nula_revienta() {
            assertThatThrownBy(() -> valido().severity(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("severity is required");
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
        @DisplayName("affectedSubjectCount negativo revienta")
        void affected_subject_count_negativo_revienta() {
            assertThatThrownBy(() -> valido().affectedSubjectCount(-1).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("affectedSubjectCount must not be negative");
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
    @DisplayName("linea de tiempo — occurredAt <= detectedAt <= escalatedAt < deadlineAt")
    class LineaDeTiempo {

        @Test
        @DisplayName("occurredAt posterior a detectedAt revienta")
        void occurred_at_posterior_a_detected_at_revienta() {
            assertThatThrownBy(() -> valido()
                    .occurredAt(SecurityIncidentMother.DETECTED_AT.plusDays(1)).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("occurredAt must not be after detectedAt");
        }

        @Test
        @DisplayName("escalatedAt nulo revienta: es el punto de partida del plazo legal")
        void escalated_at_nulo_revienta() {
            assertThatThrownBy(() -> valido().escalatedAt(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("escalatedAt is required");
        }

        @Test
        @DisplayName("escalatedAt anterior a detectedAt revienta")
        void escalated_at_anterior_a_detected_at_revienta() {
            assertThatThrownBy(() -> valido()
                    .escalatedAt(SecurityIncidentMother.DETECTED_AT.minusDays(1)).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("escalatedAt must not be before detectedAt");
        }

        @Test
        @DisplayName("deadlineAt nulo revienta")
        void deadline_at_nulo_revienta() {
            assertThatThrownBy(() -> valido().deadlineAt(null).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deadlineAt is required");
        }

        @Test
        @DisplayName("deadlineAt igual a escalatedAt revienta: tiene que ser ESTRICTAMENTE posterior")
        void deadline_at_igual_a_escalated_at_revienta() {
            assertThatThrownBy(
                    () -> valido().deadlineAt(SecurityIncidentMother.ESCALATED_AT).build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("deadlineAt must be after escalatedAt");
        }
    }

    @Nested
    @DisplayName("reporte a la autoridad")
    class Reporte {

        @Test
        @DisplayName("el primer reporte deja fecha y radicado, y conserva el resto del incidente")
        void el_primer_reporte_deja_fecha_y_radicado() {
            SecurityIncident registrado = SecurityIncidentMother.registrado();

            SecurityIncident reportado = registrado.report(SecurityIncidentMother.REPORTED_AT,
                    SecurityIncidentMother.REPORT_REFERENCE);

            assertThat(reportado.getReportedToAuthorityAt())
                    .isEqualTo(SecurityIncidentMother.REPORTED_AT);
            assertThat(reportado.getReportReference())
                    .isEqualTo(SecurityIncidentMother.REPORT_REFERENCE);
            assertThat(reportado.isReported()).isTrue();
            assertThat(reportado.getId()).isEqualTo(registrado.getId());
            assertThat(reportado.getSummary()).isEqualTo(registrado.getSummary());
            assertThat(reportado.isClosed()).isFalse();
        }

        @Test
        @DisplayName("reportar dos veces revienta: la fecha ya presentada no se reescribe")
        void reportar_dos_veces_revienta() {
            SecurityIncident reportado = SecurityIncidentMother.reportado();

            assertThatThrownBy(() -> reportado
                    .report(SecurityIncidentMother.REPORTED_AT.plusDays(1), "OTRO-RADICADO"))
                    .isInstanceOf(SecurityIncidentAlreadyReportedException.class)
                    .hasMessageContaining(
                            "Security incident " + SecurityIncidentMother.INCIDENT_ID);
        }

        @Test
        @DisplayName("un radicado sin fecha de reporte revienta")
        void radicado_sin_fecha_de_reporte_revienta() {
            SecurityIncident registrado = SecurityIncidentMother.registrado();

            assertThatThrownBy(
                    () -> registrado.report(null, SecurityIncidentMother.REPORT_REFERENCE))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "reportReference must be absent while the incident is not reported");
        }

        @Test
        @DisplayName("una fecha de reporte sin radicado revienta: un reporte sin rastro no consta")
        void fecha_de_reporte_sin_radicado_revienta() {
            SecurityIncident registrado = SecurityIncidentMother.registrado();

            assertThatThrownBy(() -> registrado.report(SecurityIncidentMother.REPORTED_AT, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reportReference is required once reported");
        }

        @Test
        @DisplayName("un radicado por encima de 100 caracteres revienta")
        void radicado_supera_100_caracteres_revienta() {
            SecurityIncident registrado = SecurityIncidentMother.registrado();

            assertThatThrownBy(
                    () -> registrado.report(SecurityIncidentMother.REPORTED_AT, "R".repeat(101)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reportReference must be 100 chars or less");
        }

        @Test
        @DisplayName("una fecha de reporte anterior a la deteccion revienta")
        void fecha_de_reporte_anterior_a_la_deteccion_revienta() {
            SecurityIncident registrado = SecurityIncidentMother.registrado();

            assertThatThrownBy(
                    () -> registrado.report(SecurityIncidentMother.DETECTED_AT.minusDays(1),
                            SecurityIncidentMother.REPORT_REFERENCE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("reportedToAuthorityAt must not be before detectedAt");
        }
    }

    @Nested
    @DisplayName("cierre")
    class Cierre {

        @Test
        @DisplayName("el cierre escribe contencion y causa raiz, y conserva lo ya reportado")
        void el_cierre_escribe_contencion_y_causa_raiz() {
            SecurityIncident reportado = SecurityIncidentMother.reportado();

            SecurityIncident cerrado = reportado.close(SecurityIncidentMother.CLOSED_AT,
                    SecurityIncidentMother.CONTAINMENT, SecurityIncidentMother.ROOT_CAUSE,
                    SecurityIncidentMother.NOTIFIED_SUBJECTS_AT);

            assertThat(cerrado.getContainment()).isEqualTo(SecurityIncidentMother.CONTAINMENT);
            assertThat(cerrado.getRootCause()).isEqualTo(SecurityIncidentMother.ROOT_CAUSE);
            assertThat(cerrado.getClosedAt()).isEqualTo(SecurityIncidentMother.CLOSED_AT);
            assertThat(cerrado.getNotifiedSubjectsAt())
                    .isEqualTo(SecurityIncidentMother.NOTIFIED_SUBJECTS_AT);
            assertThat(cerrado.isClosed()).isTrue();
            assertThat(cerrado.getReportedToAuthorityAt())
                    .isEqualTo(SecurityIncidentMother.REPORTED_AT);
        }

        @Test
        @DisplayName("cerrar sin notificar a los titulares es legitimo: es opcional")
        void cerrar_sin_notificar_a_los_titulares_es_legitimo() {
            SecurityIncident cerrado = SecurityIncidentMother.registrado().close(
                    SecurityIncidentMother.CLOSED_AT, SecurityIncidentMother.CONTAINMENT,
                    SecurityIncidentMother.ROOT_CAUSE, null);

            assertThat(cerrado.getNotifiedSubjectsAt()).isNull();
            assertThat(cerrado.isClosed()).isTrue();
        }

        @Test
        @DisplayName("cerrar dos veces revienta: la narracion escrita no se reescribe")
        void cerrar_dos_veces_revienta() {
            SecurityIncident cerrado = SecurityIncidentMother.cerrado();

            assertThatThrownBy(() -> cerrado.close(SecurityIncidentMother.CLOSED_AT.plusDays(1),
                    "otra contencion", "otra causa", null))
                    .isInstanceOf(SecurityIncidentAlreadyClosedException.class)
                    .hasMessageContaining(
                            "Security incident " + SecurityIncidentMother.INCIDENT_ID);
        }

        @Test
        @DisplayName("cerrar sin contencion revienta")
        void cerrar_sin_contencion_revienta() {
            SecurityIncident registrado = SecurityIncidentMother.registrado();

            assertThatThrownBy(() -> registrado.close(SecurityIncidentMother.CLOSED_AT, null,
                    SecurityIncidentMother.ROOT_CAUSE, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("containment is required to close an incident");
        }

        @Test
        @DisplayName("cerrar sin causa raiz revienta")
        void cerrar_sin_causa_raiz_revienta() {
            SecurityIncident registrado = SecurityIncidentMother.registrado();

            assertThatThrownBy(() -> registrado.close(SecurityIncidentMother.CLOSED_AT,
                    SecurityIncidentMother.CONTAINMENT, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rootCause is required to close an incident");
        }

        @Test
        @DisplayName("cerrar antes de la deteccion revienta")
        void cerrar_antes_de_la_deteccion_revienta() {
            SecurityIncident registrado = SecurityIncidentMother.registrado();

            assertThatThrownBy(
                    () -> registrado.close(SecurityIncidentMother.DETECTED_AT.minusDays(1),
                            SecurityIncidentMother.CONTAINMENT, SecurityIncidentMother.ROOT_CAUSE,
                            null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("closedAt must not be before detectedAt");
        }
    }

    @Nested
    @DisplayName("consultas")
    class Consultas {

        @Test
        @DisplayName("un incidente recien registrado no esta reportado ni cerrado")
        void un_incidente_recien_registrado_no_esta_reportado_ni_cerrado() {
            SecurityIncident registrado = SecurityIncidentMother.registrado();

            assertThat(registrado.isReported()).isFalse();
            assertThat(registrado.isClosed()).isFalse();
        }

        @Test
        @DisplayName("isOverdue es verdadero cuando no se ha reportado y ya paso el plazo")
        void is_overdue_true_cuando_no_se_ha_reportado_y_paso_el_plazo() {
            SecurityIncident registrado = SecurityIncidentMother.registrado();

            assertThat(registrado.isOverdue(SecurityIncidentMother.DEADLINE_AT.plusMinutes(1)))
                    .isTrue();
        }

        @Test
        @DisplayName("isOverdue es falso mientras el plazo no ha vencido")
        void is_overdue_false_mientras_el_plazo_no_ha_vencido() {
            SecurityIncident registrado = SecurityIncidentMother.registrado();

            assertThat(registrado.isOverdue(SecurityIncidentMother.DEADLINE_AT.minusMinutes(1)))
                    .isFalse();
        }

        @Test
        @DisplayName("isOverdue es falso una vez reportado, aunque la fecha consultada sea posterior al plazo")
        void is_overdue_false_una_vez_reportado() {
            SecurityIncident reportado = SecurityIncidentMother.reportado();

            assertThat(reportado.isOverdue(SecurityIncidentMother.DEADLINE_AT.plusDays(10)))
                    .isFalse();
        }
    }
}
