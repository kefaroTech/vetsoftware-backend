package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.pricelist.infrastructure.persistence.PriceListJpaRepository;
import com.vetsoftware.app.subscription.application.port.out.PriceListValidationPort;
import org.springframework.stereotype.Component;

@Component("subscriptionJpaPriceListValidationPort")
public class JpaPriceListValidationPort implements PriceListValidationPort {

    private final PriceListJpaRepository priceListJpaRepository;

    public JpaPriceListValidationPort(PriceListJpaRepository priceListJpaRepository) {
        this.priceListJpaRepository = priceListJpaRepository;
    }

    @Override
    public void validateExists(Long priceListId) {
        if (priceListId == null || !priceListJpaRepository.existsById(priceListId)) {
            throw new IllegalArgumentException("Price list not found: " + priceListId);
        }
    }
}
