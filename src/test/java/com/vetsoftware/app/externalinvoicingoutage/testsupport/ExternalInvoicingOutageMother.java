package com.vetsoftware.app.externalinvoicingoutage.testsupport;

import com.vetsoftware.app.externalinvoicingoutage.application.command.EndExternalInvoicingOutageCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.command.NotifyAffectedCompaniesCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.command.OpenExternalInvoicingOutageCommand;
import com.vetsoftware.app.externalinvoicingoutage.application.command.RegisterAffectedCompanyCommand;
import com.vetsoftware.app.externalinvoicingoutage.domain.CauseParty;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageCompany;
import com.vetsoftware.app.externalinvoicingoutage.domain.OutageResolution;
import java.time.LocalDateTime;

/**
 * Fixtures de la rodaja externalinvoicingoutage.
 *
 * <p>
 * {@code startedAt}, {@code notifiedCompaniesAt} y {@code endedAt} caen en dias
 * DISTINTOS a proposito, igual que en {@code SecurityIncidentMother}: repetir
 * el mismo dia en dos campos dejaria de ejercitar el orden temporal que
 * {@link ExternalInvoicingOutage} exige.
 */
public final class ExternalInvoicingOutageMother {

    public static final Long OUTAGE_ID = 600L;
    public static final Long VERSION = 2L;
    public static final Long COMPANY_ID = 88L;

    public static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 1, 5, 9, 0);
    public static final LocalDateTime NOTIFIED_COMPANIES_AT = LocalDateTime.of(2026, 1, 5, 20, 0);
    public static final LocalDateTime ENDED_AT = LocalDateTime.of(2026, 1, 6, 14, 30);
    public static final LocalDateTime CREATED_DATE = LocalDateTime.of(2026, 1, 5, 9, 5);

    public static final CauseParty CAUSE_PARTY = CauseParty.EXTERNAL_ISSUER;
    public static final String SUMMARY = "Caida del proveedor de facturacion electronica";
    public static final int AFFECTED_COMPANY_COUNT = 40;
    public static final int NOTIFIED_COMPANY_COUNT = 38;
    public static final String EXTERNAL_INCIDENT_REF = "PROV-2026-0042";

    public static final Long AFFECTED_ID = 950L;
    public static final int FAILED_DOCUMENT_COUNT = 7;
    public static final OutageResolution RESOLVED_BY = OutageResolution.CONTINGENCY_NUMBERING;

    private ExternalInvoicingOutageMother() {
    }

    /** Caida abierta, con id: como si ya se hubiera guardado. Sin aviso cursado. */
    public static ExternalInvoicingOutage abierta() {
        return abierta(OUTAGE_ID);
    }

    public static ExternalInvoicingOutage abierta(Long id) {
        return new ExternalInvoicingOutage(id, STARTED_AT, null, CAUSE_PARTY, SUMMARY,
                AFFECTED_COMPANY_COUNT, null, EXTERNAL_INCIDENT_REF, CREATED_DATE, VERSION);
    }

    /**
     * Abierta y ya con el aviso cursado a las clinicas, con el contador corregido.
     */
    public static ExternalInvoicingOutage abiertaNotificada() {
        return new ExternalInvoicingOutage(OUTAGE_ID, STARTED_AT, null, CAUSE_PARTY, SUMMARY,
                NOTIFIED_COMPANY_COUNT, NOTIFIED_COMPANIES_AT, EXTERNAL_INCIDENT_REF, CREATED_DATE,
                VERSION);
    }

    /** Cerrada: con hora de fin y aviso cursado. */
    public static ExternalInvoicingOutage cerrada() {
        return new ExternalInvoicingOutage(OUTAGE_ID, STARTED_AT, ENDED_AT, CAUSE_PARTY, SUMMARY,
                NOTIFIED_COMPANY_COUNT, NOTIFIED_COMPANIES_AT, EXTERNAL_INCIDENT_REF, CREATED_DATE,
                VERSION);
    }

    public static ExternalInvoicingOutageCompany afectada() {
        return new ExternalInvoicingOutageCompany(AFFECTED_ID, OUTAGE_ID, COMPANY_ID,
                FAILED_DOCUMENT_COUNT, RESOLVED_BY);
    }

    public static OpenExternalInvoicingOutageCommand comandoAbrir() {
        return new OpenExternalInvoicingOutageCommand(STARTED_AT, CAUSE_PARTY, SUMMARY,
                AFFECTED_COMPANY_COUNT, EXTERNAL_INCIDENT_REF);
    }

    public static EndExternalInvoicingOutageCommand comandoCerrar() {
        return new EndExternalInvoicingOutageCommand(OUTAGE_ID, ENDED_AT);
    }

    public static NotifyAffectedCompaniesCommand comandoNotificar() {
        return new NotifyAffectedCompaniesCommand(OUTAGE_ID, NOTIFIED_COMPANIES_AT,
                NOTIFIED_COMPANY_COUNT);
    }

    public static RegisterAffectedCompanyCommand comandoRegistrarAfectada() {
        return new RegisterAffectedCompanyCommand(OUTAGE_ID, COMPANY_ID, FAILED_DOCUMENT_COUNT,
                RESOLVED_BY);
    }
}
