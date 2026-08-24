package com.vetsoftware.app.subscriptionbilling.infrastructure.web.response;

import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lo devengado, tal como lo ven los frontends.
 *
 * <p>
 * {@code prorationDays} y {@code periodDays} salen en la respuesta y no solo en
 * la base: son los dos números con los que una pantalla puede explicar un
 * prorrateo sin que nadie tenga que reconstruirlo.
 *
 * <p>
 * <b>No hay ningún importe de impuesto aquí</b>, y no es un campo que falte: el
 * cargo guarda su base y su tarifa, y el impuesto vive calculado una sola vez
 * en el desglose del documento.
 */
public record SubscriptionChargeResponse(Long id, Long subscriptionId, Long subscriptionItemId,
        ChargeType chargeType, String description, LocalDate servicePeriodStart,
        LocalDate servicePeriodEnd, BigDecimal quantity, BigDecimal unitAmount,
        BigDecimal subtotalAmount, BigDecimal taxRate, TaxTreatment taxTreatment,
        Integer prorationDays, Integer periodDays, ChargeStatus status, Long amendmentId,
        Long billingDocumentId, Long voidsChargeId, LocalDateTime createdDate) {

    public static SubscriptionChargeResponse from(SubscriptionChargeDto dto) {
        return new SubscriptionChargeResponse(dto.id(), dto.subscriptionId(),
                dto.subscriptionItemId(), dto.chargeType(), dto.description(),
                dto.servicePeriodStart(), dto.servicePeriodEnd(), dto.quantity(), dto.unitAmount(),
                dto.subtotalAmount(), dto.taxRate(), dto.taxTreatment(), dto.prorationDays(),
                dto.periodDays(), dto.status(), dto.amendmentId(), dto.billingDocumentId(),
                dto.voidsChargeId(), dto.createdDate());
    }
}
