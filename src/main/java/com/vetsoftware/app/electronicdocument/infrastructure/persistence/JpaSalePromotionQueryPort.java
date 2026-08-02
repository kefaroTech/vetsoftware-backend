package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.electronicdocument.application.port.out.SalePromotionQueryPort;
import com.vetsoftware.app.promotion.infrastructure.persistence.PromotionJpaEntity;
import com.vetsoftware.app.promotion.infrastructure.persistence.PromotionJpaRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter: trae las promociones de la empresa (filtradas por enabled=true
 * via @SQLRestriction en PromotionJpaEntity) y se queda con las ACTIVAS en la
 * fecha dada (estado ACTIVE + hoy dentro del rango [startDate, endDate]),
 * espejando la derivacion de estado del front (pricing.ts#promoStatus). Mapea a
 * la forma local de SalePromotionQueryPort por nombre de enum para no acoplar
 * el dominio de promotion.
 */
@Component
public class JpaSalePromotionQueryPort implements SalePromotionQueryPort {
    private final PromotionJpaRepository promotionRepository;

    public JpaSalePromotionQueryPort(PromotionJpaRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    @Override
    public List<SalePromotion> findActive(Long companyId, LocalDate today) {
        return promotionRepository.findAllByCompanyId(companyId).stream()
                .filter(p -> isActiveOn(p, today)).map(JpaSalePromotionQueryPort::toLocal).toList();
    }

    private static boolean isActiveOn(PromotionJpaEntity p, LocalDate today) {
        if (!"ACTIVE".equals(p.getPromotionStatus().name()))
            return false;
        LocalDate from = p.getStartDate().toLocalDate();
        LocalDate to = p.getEndDate().toLocalDate();
        return !today.isBefore(from) && !today.isAfter(to);
    }

    private static SalePromotion toLocal(PromotionJpaEntity p) {
        return new SalePromotion(PromotionType.valueOf(p.getPromotionType().name()),
                ApplicationType.valueOf(p.getApplicationType().name()), p.getApplicationItem(),
                ValueType.valueOf(p.getValueType().name()), p.getValue());
    }
}
