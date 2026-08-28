package com.vetsoftware.app.securityincident.testsupport;

import com.vetsoftware.app.securityincident.application.command.CloseSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.command.RegisterAffectedCompanyCommand;
import com.vetsoftware.app.securityincident.application.command.RegisterSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.application.command.ReportSecurityIncidentCommand;
import com.vetsoftware.app.securityincident.domain.AffectedScope;
import com.vetsoftware.app.securityincident.domain.IncidentSeverity;
import com.vetsoftware.app.securityincident.domain.SecurityIncident;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentCompany;
import com.vetsoftware.app.securityincident.domain.SecurityIncidentKind;
import java.time.LocalDateTime;

/**
 * Fixtures de la rodaja securityincident.
 *
 * <p>
 * Las fechas del ciclo de vida caen en dias DISTINTOS a proposito: un fixture
 * que repitiera el mismo dia en dos campos no ejercitaria el orden temporal que
 * {@link SecurityIncident} exige (occurredAt &lt;= detectedAt &lt;= escalatedAt
 * &lt; deadlineAt).
 */
public final class SecurityIncidentMother {

    public static final Long INCIDENT_ID = 500L;
    public static final Long VERSION = 3L;
    public static final Long COMPANY_ID = 77L;

    public static final LocalDateTime OCCURRED_AT = LocalDateTime.of(2026, 1, 9, 20, 0);
    public static final LocalDateTime DETECTED_AT = LocalDateTime.of(2026, 1, 10, 8, 0);
    public static final LocalDateTime ESCALATED_AT = LocalDateTime.of(2026, 1, 12, 9, 0);
    public static final LocalDateTime DEADLINE_AT = LocalDateTime.of(2026, 2, 2, 23, 59, 59,
            999_999_000);
    public static final LocalDateTime CREATED_DATE = LocalDateTime.of(2026, 1, 10, 8, 5);
    public static final LocalDateTime REPORTED_AT = LocalDateTime.of(2026, 1, 20, 10, 0);
    public static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 1, 25, 16, 0);
    public static final LocalDateTime NOTIFIED_SUBJECTS_AT = LocalDateTime.of(2026, 1, 26, 9, 0);

    public static final SecurityIncidentKind KIND = SecurityIncidentKind.UNAUTHORIZED_ACCESS;
    public static final IncidentSeverity SEVERITY = IncidentSeverity.HIGH;
    public static final String SUMMARY = "Acceso no autorizado detectado en el portal de citas";
    public static final int AFFECTED_SUBJECT_COUNT = 42;
    public static final String REPORT_REFERENCE = "SIC-2026-000123";
    public static final String CONTAINMENT = "Se revocaron las credenciales comprometidas y se roto el secreto de firma";
    public static final String ROOT_CAUSE = "Reutilizacion de una contrasena filtrada en otro servicio";

    public static final Long AFFECTED_ID = 900L;
    public static final AffectedScope AFFECTED_SCOPE = AffectedScope.PERSONAL_DATA;
    public static final int COMPANY_AFFECTED_SUBJECT_COUNT = 12;

    private SecurityIncidentMother() {
    }

    /**
     * Recien registrado: sin reportar y sin cerrar. Con id, como si ya se hubiera
     * guardado.
     */
    public static SecurityIncident registrado() {
        return registrado(INCIDENT_ID);
    }

    public static SecurityIncident registrado(Long id) {
        return new SecurityIncident(id, DETECTED_AT, OCCURRED_AT, ESCALATED_AT, KIND, SEVERITY,
                SUMMARY, AFFECTED_SUBJECT_COUNT, DEADLINE_AT, null, null, null, null, null, null,
                CREATED_DATE, VERSION);
    }

    /** Reportado a la autoridad, todavia sin cerrar. */
    public static SecurityIncident reportado() {
        return new SecurityIncident(INCIDENT_ID, DETECTED_AT, OCCURRED_AT, ESCALATED_AT, KIND,
                SEVERITY, SUMMARY, AFFECTED_SUBJECT_COUNT, DEADLINE_AT, REPORTED_AT,
                REPORT_REFERENCE, null, null, null, null, CREATED_DATE, VERSION);
    }

    /** Reportado y cerrado, con su contencion y su causa raiz. */
    public static SecurityIncident cerrado() {
        return new SecurityIncident(INCIDENT_ID, DETECTED_AT, OCCURRED_AT, ESCALATED_AT, KIND,
                SEVERITY, SUMMARY, AFFECTED_SUBJECT_COUNT, DEADLINE_AT, REPORTED_AT,
                REPORT_REFERENCE, NOTIFIED_SUBJECTS_AT, CONTAINMENT, ROOT_CAUSE, CLOSED_AT,
                CREATED_DATE, VERSION);
    }

    public static SecurityIncidentCompany afectada() {
        return new SecurityIncidentCompany(AFFECTED_ID, INCIDENT_ID, COMPANY_ID, AFFECTED_SCOPE,
                COMPANY_AFFECTED_SUBJECT_COUNT);
    }

    public static RegisterSecurityIncidentCommand comandoRegistrar() {
        return new RegisterSecurityIncidentCommand(DETECTED_AT, OCCURRED_AT, ESCALATED_AT, KIND,
                SEVERITY, SUMMARY, AFFECTED_SUBJECT_COUNT);
    }

    public static CloseSecurityIncidentCommand comandoCerrar() {
        return new CloseSecurityIncidentCommand(INCIDENT_ID, CLOSED_AT, CONTAINMENT, ROOT_CAUSE,
                NOTIFIED_SUBJECTS_AT);
    }

    public static ReportSecurityIncidentCommand comandoReportar() {
        return new ReportSecurityIncidentCommand(INCIDENT_ID, REPORTED_AT, REPORT_REFERENCE);
    }

    public static RegisterAffectedCompanyCommand comandoRegistrarAfectada() {
        return new RegisterAffectedCompanyCommand(INCIDENT_ID, COMPANY_ID, AFFECTED_SCOPE,
                COMPANY_AFFECTED_SUBJECT_COUNT);
    }
}
