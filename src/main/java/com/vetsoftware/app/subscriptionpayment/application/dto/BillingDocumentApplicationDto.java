package com.vetsoftware.app.subscriptionpayment.application.dto;

import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.BillingDocumentApplication;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Una aplicacion tal como sale de la capa de aplicacion.
 *
 * <p>
 * <b>Lleva la referencia de los cuatro origenes nuevos, y eso no es
 * completitud.</b> Un {@code sourceKind = WITHHOLDING} sin
 * {@code withholdingId} le dice a la pantalla que la factura se saldo con una
 * retencion y no le dice cual: quien reclame el certificado tiene que buscarlo
 * a mano. Y un {@code WRITE_OFF} sin su autorizante ni su motivo es dinero que
 * desaparecio sin nadie detras, que es exactamente lo que la firma nominal
 * existe para evitar.
 */
public record BillingDocumentApplicationDto(Long id, Long companyId,
        BillingDocumentSummaryDto targetDocument, ApplicationSourceKind sourceKind, Long paymentId,
        BillingDocumentSummaryDto sourceDocument, Long withholdingId, Long creditEntryId,
        BigDecimal appliedAmount, Long reversalOfId, Long writeOffAuthorizedBySystemUserId,
        String writeOffReason, LocalDateTime appliedAt, LocalDate valueDate,
        LocalDateTime createdDate) {

    public static BillingDocumentApplicationDto from(BillingDocumentApplication application) {
        return new BillingDocumentApplicationDto(application.getId(), application.getCompanyId(),
                BillingDocumentSummaryDto.from(application.getTargetDocument()),
                application.getSourceKind(), application.getPaymentId(),
                application.getSourceDocument() == null
                        ? null
                        : BillingDocumentSummaryDto.from(application.getSourceDocument()),
                application.getWithholdingId(), application.getCreditEntryId(),
                application.getAppliedAmount(), application.getReversalOfId(),
                application.getWriteOffAuthorizedBySystemUserId(), application.getWriteOffReason(),
                application.getAppliedAt(), application.getValueDate(),
                application.getCreatedDate());
    }
}
