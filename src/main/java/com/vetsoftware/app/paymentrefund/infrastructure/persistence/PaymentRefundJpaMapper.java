package com.vetsoftware.app.paymentrefund.infrastructure.persistence;

import com.vetsoftware.app.paymentrefund.domain.PaymentRefund;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Un solo {@code toDomain} y sin sobrecarga de camino de
 * escritura</strong>: el dominio no guarda ningun companion VO, solo los ids de
 * las FK, asi que no hay proxy que se pueda disparar al reconstruir la
 * devolucion.
 */
@Component
public class PaymentRefundJpaMapper {

    public PaymentRefundJpaEntity toJpa(PaymentRefund refund) {
        PaymentRefundJpaEntity entity = new PaymentRefundJpaEntity();
        entity.setId(refund.getId());
        entity.setCompanyId(refund.getCompanyId());
        entity.setPaymentId(refund.getPaymentId());
        entity.setSourceDocumentId(refund.getSourceDocumentId());
        entity.setAmount(refund.getAmount());
        entity.setMethod(refund.getMethod());
        entity.setDestinationReference(refund.getDestinationReference());
        entity.setRefundedAt(refund.getRefundedAt());
        entity.setValueDate(refund.getValueDate());
        entity.setReasonCode(refund.getReasonCode());
        entity.setReason(refund.getReason());
        entity.setAuthorizedBySystemUserId(refund.getAuthorizedBySystemUserId());
        entity.setClientRequestId(refund.getClientRequestId());
        entity.setCreatedDate(refund.getCreatedDate());
        return entity;
    }

    public PaymentRefund toDomain(PaymentRefundJpaEntity entity) {
        return new PaymentRefund(entity.getId(), entity.getCompanyId(), entity.getPaymentId(),
                entity.getSourceDocumentId(), entity.getAmount(), entity.getMethod(),
                entity.getDestinationReference(), entity.getRefundedAt(), entity.getValueDate(),
                entity.getReasonCode(), entity.getReason(), entity.getAuthorizedBySystemUserId(),
                entity.getClientRequestId(), entity.getCreatedDate());
    }
}
