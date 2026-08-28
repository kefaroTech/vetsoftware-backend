package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.subscription.application.dto.InitialCapacityTemplate;
import com.vetsoftware.app.subscription.application.dto.InitialContractTemplate;
import com.vetsoftware.app.subscription.application.port.out.PlatformCatalogPort;
import com.vetsoftware.app.subscription.domain.BillingCycle;
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
                // modulo (chk_subscription_items_capacity_unit). Desde el 333 el valor
                // pasa tal cual: es el codigo del eje, no un enumerado que traducir.
                row.getCapacityUnit(), orZero(row.getIncludedQuantity()),
                orZero(row.getMinQuantity()), row.getUnitAmount(), row.getTaxRate(),
                TaxTreatment.valueOf(row.getTaxTreatment()), orZero(row.getDefaultGraceDays()),
                orZero(row.getDefaultTrialDays()));
    }

    /**
     * La contraria de la de arriba: aqui la unidad <strong>no</strong> puede venir
     * nula. La consulta ya filtra {@code capacity_unit IS NOT NULL}, asi que un
     * {@code null} en este punto significaria que alguien aflojo ese filtro.
     *
     * <p>
     * <strong>La comprobacion va explicita desde el changeset 333.</strong> Antes
     * la hacia gratis {@code CapacityUnit.valueOf(null)}, que lanzaba
     * {@code NullPointerException} con la fila delante; ahora el valor es una
     * cadena que pasa tal cual, asi que un nulo se colaria hasta el constructor de
     * {@code SubscriptionItem} —tres capas mas adelante y sin decir de que
     * articulo—. Se falla aqui, nombrando el articulo, que es donde se puede
     * arreglar.
     */
    private static InitialCapacityTemplate toCapacity(InitialContractRow row) {
        if (row.getCapacityUnit() == null || row.getCapacityUnit().isBlank())
            throw new IllegalStateException("catalog_items row " + row.getCatalogItemId() + " ("
                    + row.getItemCode() + ") is a core CAPACITY item with no capacity_unit:"
                    + " the query that filters capacity_unit IS NOT NULL was loosened");
        return new InitialCapacityTemplate(row.getCatalogItemId(), row.getItemCode(),
                row.getItemName(), row.getCapacityUnit(), orZero(row.getIncludedQuantity()),
                orZero(row.getMinQuantity()), row.getUnitAmount(), row.getTaxRate(),
                TaxTreatment.valueOf(row.getTaxTreatment()));
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }
}
