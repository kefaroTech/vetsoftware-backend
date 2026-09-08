package com.vetsoftware.app.registration.infrastructure.persistence;

import com.vetsoftware.app.registration.application.port.out.EligibleTrialCatalogItemsPort;
import com.vetsoftware.app.subscription.application.dto.EligibleTrialItemTemplate;
import com.vetsoftware.app.subscription.application.port.out.PlatformCatalogPort;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Traduce a la forma de {@code registration} la fila que ya resuelve
 * {@link PlatformCatalogPort#findEligibleTrialItems}: precio e IVA se descartan
 * aquí porque una concesión no negocia tarifa, solo días y desenlace.
 */
@Component
public class JpaEligibleTrialCatalogItemsPort implements EligibleTrialCatalogItemsPort {

    private final PlatformCatalogPort platformCatalogPort;

    public JpaEligibleTrialCatalogItemsPort(PlatformCatalogPort platformCatalogPort) {
        this.platformCatalogPort = platformCatalogPort;
    }

    @Override
    public List<EligibleTrialCatalogItem> findAll(BillingCycle billingCycle) {
        return platformCatalogPort.findEligibleTrialItems(billingCycle).stream()
                .map(JpaEligibleTrialCatalogItemsPort::toCompanionItem).toList();
    }

    private static EligibleTrialCatalogItem toCompanionItem(EligibleTrialItemTemplate item) {
        return new EligibleTrialCatalogItem(item.catalogItemId(), item.defaultTrialDays(),
                item.trialOutcome());
    }
}
