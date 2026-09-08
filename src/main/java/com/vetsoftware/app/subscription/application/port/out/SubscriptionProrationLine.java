package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * El prorrateo de UNA línea de un otrosí, listo para convertirse en un cargo de
 * facturación. Vive en {@code subscription} porque es lo único de ese cargo que
 * este slice conoce sin cruzar a {@code subscriptionbilling}; el adaptador que
 * consume {@link SubscriptionProrationChargePort} es quien lo traduce.
 */
public record SubscriptionProrationLine(Long companyId, Long subscriptionId,
        Long subscriptionItemId, String description, LocalDate servicePeriodStart,
        LocalDate servicePeriodEnd, BigDecimal quantity, BigDecimal unitAmount,
        BigDecimal subtotalAmount, BigDecimal taxRate, TaxTreatment taxTreatment, int prorationDays,
        int periodDays, Long amendmentId) {
}
