package com.vetsoftware.app.subscriptionitemlimit.domain;

/** No hay techo congelado para esa línea de contrato y ese eje. */
public class SubscriptionItemLimitNotFoundException extends RuntimeException {

    public SubscriptionItemLimitNotFoundException(Long companyId, Long subscriptionItemId,
            Long limitDimensionId) {
        super("Company " + companyId + " has no frozen limit for subscription item "
                + subscriptionItemId + " and dimension " + limitDimensionId);
    }
}
