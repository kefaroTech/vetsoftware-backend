package com.vetsoftware.app.subscriptionbilling.application.dto;

import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.ProrationBasis;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lo devengado, tal como sale de la aplicación.
 *
 * <p>
 * Lleva {@code prorationDays} y {@code periodDays} <b>siempre que existan</b>:
 * son los dos números que permiten explicarle un prorrateo a un cliente que
 * reclama sin tener que reconstruirlo a mano. Y no lleva ningún importe de
 * impuesto, porque el cargo guarda su base, no su impuesto.
 */
public record SubscriptionChargeDto(Long id, Long companyId, Long subscriptionId,
        Long subscriptionItemId, ChargeType chargeType, String description,
        LocalDate servicePeriodStart, LocalDate servicePeriodEnd, BigDecimal quantity,
        BigDecimal unitAmount, BigDecimal subtotalAmount, BigDecimal taxRate,
        TaxTreatment taxTreatment, Integer prorationDays, Integer periodDays, ChargeStatus status,
        Long amendmentId, Long billingDocumentId, Long voidsChargeId, LocalDateTime createdDate) {

    public static SubscriptionChargeDto from(SubscriptionCharge charge) {
        ProrationBasis proration = charge.getProration();
        return new SubscriptionChargeDto(charge.getId(), charge.getCompanyId(),
                charge.getSubscriptionId(), charge.getSubscriptionItemId(), charge.getChargeType(),
                charge.getDescription(), charge.getServicePeriod().start(),
                charge.getServicePeriod().end(), charge.getQuantity(), charge.getUnitAmount(),
                charge.getSubtotalAmount(), charge.getTaxRate(), charge.getTaxTreatment(),
                proration == null ? null : proration.prorationDays(),
                proration == null ? null : proration.periodDays(), charge.getStatus(),
                charge.getAmendmentId(), charge.getBillingDocumentId(), charge.getVoidsChargeId(),
                charge.getCreatedDate());
    }
}
