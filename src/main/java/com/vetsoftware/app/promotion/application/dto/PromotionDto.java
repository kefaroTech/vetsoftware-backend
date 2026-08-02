package com.vetsoftware.app.promotion.application.dto;

import com.vetsoftware.app.promotion.domain.ApplicationType;
import com.vetsoftware.app.promotion.domain.Promotion;
import com.vetsoftware.app.promotion.domain.PromotionStatus;
import com.vetsoftware.app.promotion.domain.PromotionType;
import com.vetsoftware.app.promotion.domain.ValueType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionDto(Long id, String name, PromotionType promotionType,
        ApplicationType applicationType, Long applicationItem, ValueType valueType,
        BigDecimal value, LocalDateTime startDate, LocalDateTime endDate,
        PromotionStatus promotionStatus, CompanySummaryDto company, LocalDateTime createdDate,
        boolean enabled) {
    public static PromotionDto from(Promotion promotion) {
        return new PromotionDto(promotion.getId(), promotion.getName(),
                promotion.getPromotionType(), promotion.getApplicationType(),
                promotion.getApplicationItem(), promotion.getValueType(), promotion.getValue(),
                promotion.getStartDate(), promotion.getEndDate(), promotion.getPromotionStatus(),
                promotion.getCompany() == null
                        ? null
                        : CompanySummaryDto.from(promotion.getCompany()),
                promotion.getCreatedDate(), promotion.isEnabled());
    }
}
