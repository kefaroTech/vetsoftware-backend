package com.vetsoftware.app.externalinvoicingoutage.application.dto;

import com.vetsoftware.app.externalinvoicingoutage.domain.CauseParty;
import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutage;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code version}</strong>: el numero de bloqueo optimista es una
 * barandilla del que escribe, no un dato de la caida. Publicarlo invitaria a un
 * cliente a enviarlo de vuelta y a construir un protocolo de concurrencia que
 * esta ficha —que solo escribe plataforma— no necesita.
 *
 * <p>
 * <strong>Tampoco lleva {@code open_outage_marker}</strong>: es detalle del
 * motor, existe para que {@code uq_eio_open} pueda restringir lo que con
 * {@code NULL} no restringia, y publicarlo invitaria a construir logica sobre
 * un centinela de base de datos. Lo que si sale es {@code open}, que es la
 * misma pregunta contestada desde el modelo.
 */
public record ExternalInvoicingOutageDto(Long id, LocalDateTime startedAt, LocalDateTime endedAt,
        CauseParty causeParty, String summary, int affectedCompanyCount,
        LocalDateTime notifiedCompaniesAt, String externalIncidentRef, boolean open,
        LocalDateTime createdDate) {

    public static ExternalInvoicingOutageDto from(ExternalInvoicingOutage outage) {
        return new ExternalInvoicingOutageDto(outage.getId(), outage.getStartedAt(),
                outage.getEndedAt(), outage.getCauseParty(), outage.getSummary(),
                outage.getAffectedCompanyCount(), outage.getNotifiedCompaniesAt(),
                outage.getExternalIncidentRef(), outage.isOpen(), outage.getCreatedDate());
    }
}
