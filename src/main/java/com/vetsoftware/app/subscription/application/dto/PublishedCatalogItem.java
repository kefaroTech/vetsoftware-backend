package com.vetsoftware.app.subscription.application.dto;

import com.vetsoftware.app.subscription.domain.ContractPriceTier;
import com.vetsoftware.app.subscription.domain.SubscriptionItemType;
import java.util.List;

/**
 * El articulo publicado con TODOS sus tramos de precio, resuelto en servidor.
 *
 * <p>
 * <b>Trae los tramos y no "el precio"</b>: desde D-66 la cantidad se reparte
 * acumulativamente entre ellos, y una consulta que ya haya elegido uno solo no
 * deja hacer esa cuenta. Quien reparte es
 * {@link com.vetsoftware.app.subscription.domain.ContractPriceTiers}.
 */
public record PublishedCatalogItem(Long catalogItemId, String itemCode, String itemName,
        SubscriptionItemType itemType, String capacityUnit, List<ContractPriceTier> tiers) {

    public PublishedCatalogItem {
        tiers = tiers == null ? List.of() : List.copyOf(tiers);
    }
}
