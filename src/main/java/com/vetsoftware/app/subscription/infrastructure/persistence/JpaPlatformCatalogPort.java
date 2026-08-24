package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.application.dto.InitialCapacityTemplate;
import com.vetsoftware.app.subscription.application.dto.InitialContractTemplate;
import com.vetsoftware.app.subscription.application.port.out.PlatformCatalogPort;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.CapacityUnit;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import com.vetsoftware.app.subscription.infrastructure.persistence.PlatformCatalogTemplateJpaRepository.InitialContractRow;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Traduce la fila cruda del minimo estructural a los enums <strong>de este
 * slice</strong>. Es el unico sitio donde se hace esa traduccion, y es lo que
 * permite que el resto del slice no sepa que existen los enums de
 * {@code catalogitem} ni los de {@code pricelist}.
 *
 * <p>
 * Los tres vocabularios coinciden hoy —{@code MODULE}/{@code CAPACITY}/…,
 * {@code USER}/{@code BRANCH}/…, {@code TAXED}/{@code EXEMPT}/…— porque el
 * esquema los declara con los mismos {@code CHECK}. Si algun dia divergieran,
 * {@code valueOf} fallaria <em>aqui</em> y no en mitad de un alta a medio
 * hacer, que es donde tiene que fallar.
 */
@Component("subscriptionJpaPlatformCatalogPort")
public class JpaPlatformCatalogPort implements PlatformCatalogPort {

    private final PlatformCatalogTemplateJpaRepository templateJpaRepository;

    public JpaPlatformCatalogPort(PlatformCatalogTemplateJpaRepository templateJpaRepository) {
        this.templateJpaRepository = templateJpaRepository;
    }

    @Override
    public Optional<InitialContractTemplate> findInitialContractTemplate(
            BillingCycle billingCycle) {
        if (billingCycle == null)
            return Optional.empty();
        return templateJpaRepository.findInitialContractTemplate(billingCycle.name())
                .map(JpaPlatformCatalogPort::toTemplate);
    }

    @Override
    public List<InitialCapacityTemplate> findInitialCapacityTemplates(BillingCycle billingCycle) {
        if (billingCycle == null)
            return List.of();
        return templateJpaRepository.findInitialCapacityTemplates(billingCycle.name()).stream()
                .map(JpaPlatformCatalogPort::toCapacity).toList();
    }

    @Override
    public Optional<Integer> findDefaultGraceDays() {
        return templateJpaRepository.findDefaultGraceDays();
    }

    private static InitialContractTemplate toTemplate(InitialContractRow row) {
        return new InitialContractTemplate(row.getPriceListId(), row.getCatalogItemId(),
                row.getItemCode(), row.getItemName(),
                SubscriptionItemType.valueOf(row.getItemType()),
                // El nucleo es un MODULE y no lleva unidad; la columna viene nula y asi
                // tiene que quedarse, porque el dominio rechaza una unidad colgada de un
                // modulo (chk_subscription_items_capacity_unit).
                row.getCapacityUnit() == null ? null : CapacityUnit.valueOf(row.getCapacityUnit()),
                orZero(row.getIncludedQuantity()), orZero(row.getMinQuantity()),
                row.getUnitAmount(), row.getTaxRate(), TaxTreatment.valueOf(row.getTaxTreatment()),
                orZero(row.getDefaultGraceDays()), orZero(row.getDefaultTrialDays()));
    }

    /**
     * La contraria de la de arriba: aqui la unidad <strong>no</strong> puede venir
     * nula. La consulta ya filtra {@code capacity_unit IS NOT NULL}, asi que un
     * {@code null} en este punto significaria que alguien aflojo ese filtro; se
     * deja que {@code valueOf} reviente aqui, con la fila delante, y no tres capas
     * mas adelante en el constructor de {@code SubscriptionItem}.
     */
    private static InitialCapacityTemplate toCapacity(InitialContractRow row) {
        return new InitialCapacityTemplate(row.getCatalogItemId(), row.getItemCode(),
                row.getItemName(), CapacityUnit.valueOf(row.getCapacityUnit()),
                orZero(row.getIncludedQuantity()), orZero(row.getMinQuantity()),
                row.getUnitAmount(), row.getTaxRate(), TaxTreatment.valueOf(row.getTaxTreatment()));
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
