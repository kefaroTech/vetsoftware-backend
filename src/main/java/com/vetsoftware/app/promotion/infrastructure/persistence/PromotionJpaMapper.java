package com.vetsoftware.app.promotion.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.promotion.domain.CompanyRef;
import com.vetsoftware.app.promotion.domain.Promotion;
import org.springframework.stereotype.Component;

@Component
public class PromotionJpaMapper {
    public PromotionJpaEntity toJpa(Promotion promotion, CompanyJpaEntity company) {
        PromotionJpaEntity entity = new PromotionJpaEntity();
        entity.setId(promotion.getId());
        entity.setName(promotion.getName());
        entity.setPromotionType(promotion.getPromotionType());
        entity.setApplicationType(promotion.getApplicationType());
        entity.setApplicationItem(promotion.getApplicationItem());
        entity.setValueType(promotion.getValueType());
        entity.setValue(promotion.getValue());
        entity.setStartDate(promotion.getStartDate());
        entity.setEndDate(promotion.getEndDate());
        entity.setPromotionStatus(promotion.getPromotionStatus());
        entity.setCompany(company);
        entity.setCreatedDate(promotion.getCreatedDate());
        entity.setEnabled(promotion.isEnabled());
        return entity;
    }

    public Promotion toDomain(PromotionJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
                c == null ? null : new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public Promotion toDomain(PromotionJpaEntity entity, CompanyRef companyRef) {
        return new Promotion(entity.getId(), entity.getName(), entity.getPromotionType(),
                entity.getApplicationType(), entity.getApplicationItem(), entity.getValueType(),
                entity.getValue(), entity.getStartDate(), entity.getEndDate(),
                entity.getPromotionStatus(), companyRef, entity.getCreatedDate(),
                entity.isEnabled());
    }
}
