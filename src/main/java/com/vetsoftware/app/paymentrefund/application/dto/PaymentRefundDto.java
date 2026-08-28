package com.vetsoftware.app.paymentrefund.application.dto;

import com.vetsoftware.app.paymentrefund.domain.PaymentRefund;
import com.vetsoftware.app.paymentrefund.domain.RefundMethod;
import com.vetsoftware.app.paymentrefund.domain.RefundReasonCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentRefundDto(Long id, Long companyId, Long paymentId, Long sourceDocumentId,
        BigDecimal amount, RefundMethod method, String destinationReference,
        LocalDateTime refundedAt, LocalDate valueDate, RefundReasonCode reasonCode, String reason,
        Long authorizedBySystemUserId, LocalDateTime createdDate) {

    /**
     * <strong>Sin {@code clientRequestId}</strong>: la llave de idempotencia es una
     * barandilla del que escribe, no un dato del expediente. Publicarla dejaria que
     * un lector adivinara las llaves de otros y colisionara con ellas.
     */
    public static PaymentRefundDto from(PaymentRefund refund) {
        return new PaymentRefundDto(refund.getId(), refund.getCompanyId(), refund.getPaymentId(),
                refund.getSourceDocumentId(), refund.getAmount(), refund.getMethod(),
                refund.getDestinationReference(), refund.getRefundedAt(), refund.getValueDate(),
                refund.getReasonCode(), refund.getReason(), refund.getAuthorizedBySystemUserId(),
                refund.getCreatedDate());
    }
}
