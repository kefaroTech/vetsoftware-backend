package com.vetsoftware.app.quote.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.quote.application.port.out.ModuleLineSuccessionPort;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionItemJpaEntity;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionItemJpaMapper;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionItemJpaRepository;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaEntity;
import com.vetsoftware.app.subscription.infrastructure.persistence.SubscriptionJpaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escribe la sucesión de líneas a través del agregado {@link SubscriptionItem}
 * y de {@link SubscriptionItemJpaMapper} —el mismo camino que el resto del
 * slice {@code subscription} para esta tabla—, no por SQL nativo.
 */
@Component
public class JpaModuleLineSuccessionPort implements ModuleLineSuccessionPort {

    private final SubscriptionItemJpaRepository subscriptionItemJpaRepository;
    private final SubscriptionJpaRepository subscriptionJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;
    private final SubscriptionItemJpaMapper mapper;

    public JpaModuleLineSuccessionPort(SubscriptionItemJpaRepository subscriptionItemJpaRepository,
            SubscriptionJpaRepository subscriptionJpaRepository,
            CompanyJpaRepository companyJpaRepository, SubscriptionItemJpaMapper mapper) {
        this.subscriptionItemJpaRepository = subscriptionItemJpaRepository;
        this.subscriptionJpaRepository = subscriptionJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Long> findCurrentSubscriptionId(Long companyId) {
        return currentSubscription(companyId).map(SubscriptionJpaEntity::getId);
    }

    @Override
    public Optional<LocalDate> findNextBillingDate(Long companyId) {
        return currentSubscription(companyId).map(SubscriptionJpaEntity::getNextBillingDate);
    }

    private Optional<SubscriptionJpaEntity> currentSubscription(Long companyId) {
        return subscriptionJpaRepository.findFirstByCompany_IdAndStatusIn(companyId,
                SubscriptionStatus.CURRENT);
    }

    @Override
    public Optional<CurrentLine> findCurrentLine(Long companyId, Long catalogItemId) {
        return findCurrentSubscriptionId(companyId)
                .flatMap(subscriptionId -> subscriptionItemJpaRepository
                        .findByCompany_IdAndSubscription_IdAndCatalogItemIdAndEffectiveToIsNull(
                                companyId, subscriptionId, catalogItemId)
                        .map(entity -> new CurrentLine(entity.getId(), subscriptionId,
                                entity.getChargeMode(), entity.getTrialEndDate())));
    }

    @Override
    @Transactional
    public void closeLine(Long companyId, Long itemId, LocalDate closeDate) {
        SubscriptionItemJpaEntity entity = subscriptionItemJpaRepository
                .findByIdAndCompany_Id(itemId, companyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription item not found: " + itemId));
        SubscriptionItem item = mapper.toDomain(entity);
        item.endOn(closeDate, null);
        // uq_subscription_items_current solo admite una linea abierta por articulo: el
        // cierre tiene que llegar a la base ANTES del alta de la sucesora
        // (saveAndFlush),
        // o el INSERT de succeedLine choca con la fila que todavia esta abierta. Mismo
        // orden que JpaTrialLineSuccessionPort para el vencimiento natural.
        subscriptionItemJpaRepository
                .saveAndFlush(mapper.toJpa(item, entity.getCompany(), entity.getSubscription()));
    }

    @Override
    @Transactional
    public Long succeedLine(Long companyId, Long subscriptionId, Long priorLineId,
            LocalDate closeDate, LocalDate newEffectiveFrom, LinePriceSnapshot price) {
        if (priorLineId != null) {
            closeLine(companyId, priorLineId, closeDate);
        }
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(companyId);
        SubscriptionJpaEntity subscription = subscriptionJpaRepository
                .getReferenceById(subscriptionId);
        // NEVER_FREE + maxTrialDays=0 + trialEndDate=null: esta linea sucede a una
        // TRIAL,
        // no vuelve a probarse (chk_subscription_items_max_trial_days).
        SubscriptionItem successor = SubscriptionItem.open(companyId, subscriptionId,
                price.catalogItemId(), price.itemCode(), price.itemName(),
                SubscriptionItemType.valueOf(price.itemType()), null, 1, null,
                price.includedQuantity(), TaxTreatment.valueOf(price.taxTreatment()), 1,
                price.unitAmount(), BigDecimal.ZERO, BigDecimal.ZERO, false, price.taxRate(),
                EffectivePeriod.openFrom(newEffectiveFrom), ItemOrigin.ADDON, null, "PAID",
                "NEVER_FREE", 0, null, "SELF_SERVICE", priorLineId);
        return subscriptionItemJpaRepository.save(mapper.toJpa(successor, company, subscription))
                .getId();
    }
}
