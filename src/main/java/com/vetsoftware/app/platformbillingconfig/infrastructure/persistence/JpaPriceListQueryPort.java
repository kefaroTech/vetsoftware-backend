package com.vetsoftware.app.platformbillingconfig.infrastructure.persistence;

import com.vetsoftware.app.platformbillingconfig.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.platformbillingconfig.domain.PriceListRef;
import com.vetsoftware.app.pricelist.infrastructure.persistence.PriceListJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Único punto del slice que cruza la frontera hacia {@code pricelist}, y lo
 * hace por donde el CLAUDE.md lo permite: {@code infrastructure/persistence}
 * contra el {@code JpaRepository} de la otra feature. Ni el dominio ni la capa
 * de aplicación de aquí saben que {@code pricelist} existe.
 */
@Component("platformBillingConfigJpaPriceListQueryPort")
public class JpaPriceListQueryPort implements PriceListQueryPort {
    private final PriceListJpaRepository priceListJpaRepository;

    public JpaPriceListQueryPort(PriceListJpaRepository priceListJpaRepository) {
        this.priceListJpaRepository = priceListJpaRepository;
    }

    @Override
    public Optional<PriceListRef> findById(Long priceListId) {
        return priceListJpaRepository.findById(priceListId)
                .map(e -> new PriceListRef(e.getId(), e.getCode(), e.getName()));
    }
}
