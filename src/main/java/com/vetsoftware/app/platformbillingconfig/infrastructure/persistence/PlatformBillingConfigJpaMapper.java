package com.vetsoftware.app.platformbillingconfig.infrastructure.persistence;

import com.vetsoftware.app.platformbillingconfig.domain.PlatformBillingConfig;
import com.vetsoftware.app.platformbillingconfig.domain.PriceListRef;
import org.springframework.stereotype.Component;

/**
 * Ida y vuelta entre la fila y el agregado.
 *
 * <p>
 * {@code toDomain} recibe el {@link PriceListRef} ya resuelto porque la entidad
 * solo guarda el id de la tarifa: la asociación JPA se omite a propósito (ver
 * {@link PlatformBillingConfigJpaEntity}).
 */
@Component
public class PlatformBillingConfigJpaMapper {

    public PlatformBillingConfigJpaEntity toJpa(PlatformBillingConfig config) {
        PlatformBillingConfigJpaEntity entity = new PlatformBillingConfigJpaEntity();
        entity.setId(config.getId());
        entity.setDefaultPriceListId(
                config.getDefaultPriceList() == null ? null : config.getDefaultPriceList().id());
        entity.setDefaultGraceDays(config.getDefaultGraceDays());
        entity.setDefaultTrialDays(config.getDefaultTrialDays());
        entity.setInvoiceDayOfMonth(config.getInvoiceDayOfMonth());
        entity.setDefaultPaymentTermDays(config.getDefaultPaymentTermDays());
        entity.setExternalBillingProvider(config.getExternalBillingProvider());
        entity.setCreatedDate(config.getCreatedDate());
        entity.setVersion(config.getVersion());
        return entity;
    }

    public PlatformBillingConfig toDomain(PlatformBillingConfigJpaEntity entity,
            PriceListRef defaultPriceList) {
        return new PlatformBillingConfig(entity.getId(), defaultPriceList,
                entity.getDefaultGraceDays(), entity.getDefaultTrialDays(),
                entity.getInvoiceDayOfMonth(), entity.getDefaultPaymentTermDays(),
                entity.getExternalBillingProvider(), entity.getCreatedDate(), entity.getVersion());
    }
}
