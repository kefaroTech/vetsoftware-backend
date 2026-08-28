package com.vetsoftware.app.billingdocumentstatushistory.application.dto;

import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;
import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatusHistory;
import java.time.LocalDateTime;

/**
 * <strong>Sin {@code version} porque la fila no la tiene</strong>, y sin
 * {@code enabled} porque no existe: la tabla solo se agrega. Lo que si viaja
 * entero es el par {@code fromStatus}/{@code toStatus}, y los dos hacen falta —
 * con solo el destino, dos fotogramas consecutivos no se pueden empalmar y la
 * pelicula deja de poder auditarse.
 *
 * <p>
 * {@code occurredAt} y {@code createdDate} van los dos aunque hoy los escriba
 * el mismo reloj: el primero es cuando ocurrio el cambio y es por el que se
 * ordena y se corta a una fecha; el segundo es cuando quedo escrito. El dia que
 * exista un camino de carga historica dejaran de coincidir, y quien lea esto
 * tiene que poder notarlo.
 */
public record BillingDocumentStatusHistoryDto(Long id, Long companyId, Long billingDocumentId,
        BillingDocumentStatus fromStatus, BillingDocumentStatus toStatus, LocalDateTime occurredAt,
        String actor, String reason, LocalDateTime createdDate) {

    public static BillingDocumentStatusHistoryDto from(BillingDocumentStatusHistory entry) {
        return new BillingDocumentStatusHistoryDto(entry.getId(), entry.getCompanyId(),
                entry.getBillingDocumentId(), entry.getFromStatus(), entry.getToStatus(),
                entry.getOccurredAt(), entry.getActor(), entry.getReason(), entry.getCreatedDate());
    }
}
