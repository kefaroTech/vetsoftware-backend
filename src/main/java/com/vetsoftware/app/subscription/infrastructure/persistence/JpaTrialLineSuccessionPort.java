package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.application.port.out.TrialLineSuccessionPort;
import com.vetsoftware.app.subscription.domain.EffectivePeriod;
import com.vetsoftware.app.subscription.domain.ItemOrigin;
import com.vetsoftware.app.subscription.domain.SubscriptionItem;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JpaTrialLineSuccessionPort implements TrialLineSuccessionPort {

    private final TrialSubscriptionItemJpaRepository repository;
    private final SubscriptionItemJpaMapper mapper;

    public JpaTrialLineSuccessionPort(TrialSubscriptionItemJpaRepository repository,
            SubscriptionItemJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public boolean transitionIfOpen(Long companyId, Long catalogItemId, LocalDate trialEndDate,
            String successorChargeMode) {
        Optional<SubscriptionItemJpaEntity> openTrial = repository.findOpenTrialLine(companyId,
                catalogItemId);
        if (openTrial.isEmpty()) {
            return false;
        }
        SubscriptionItemJpaEntity trialEntity = openTrial.get();
        SubscriptionItem trial = mapper.toDomain(trialEntity);
        LocalDate successorFrom = trialEndDate.plusDays(1);
        trial.endOn(successorFrom, null);
        // uq_subscription_items_current solo admite una linea abierta por articulo: el
        // cierre tiene que llegar a la base ANTES del alta de la sucesora
        // (saveAndFlush),
        // o el INSERT de la sucesora choca con la fila que todavia esta abierta. Mismo
        // orden que JpaModuleLineSuccessionPort.
        repository.saveAndFlush(
                mapper.toJpa(trial, trialEntity.getCompany(), trialEntity.getSubscription()));
        // Ningun ItemOrigin describe "la prueba vencio sola"; INITIAL es el unico que
        // no afirma un evento comercial que no ocurrio.
        SubscriptionItem successor = SubscriptionItem.open(companyId, trial.getSubscriptionId(),
                trial.getCatalogItemId(), trial.getItemCode(), trial.getItemName(),
                trial.getItemType(), trial.getCapacityUnit(), trial.getTierMin(),
                trial.getTierMax(), trial.getIncludedQuantity(), trial.getTaxTreatment(),
                trial.getQuantity(), trial.getUnitAmount(), trial.getDiscountPercent(),
                trial.getDiscountAmount(), trial.isDiscountConditional(), trial.getTaxRate(),
                EffectivePeriod.openFrom(successorFrom), ItemOrigin.INITIAL, null,
                successorChargeMode, trial.getTrialEligibility(), trial.getMaxTrialDays(), null,
                trial.getActivationPath(), trial.getId());
        repository.save(
                mapper.toJpa(successor, trialEntity.getCompany(), trialEntity.getSubscription()));
        return true;
    }
}
