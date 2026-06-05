package com.vetsoftware.app.promotion.application.command;

import com.vetsoftware.app.promotion.domain.ApplicationType;
import com.vetsoftware.app.promotion.domain.PromotionStatus;
import com.vetsoftware.app.promotion.domain.PromotionType;
import com.vetsoftware.app.promotion.domain.ValueType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreatePromotionCommand(
        String name,
        PromotionType promotionType,
        ApplicationType applicationType,
        Long applicationItem,
        ValueType valueType,
        BigDecimal value,
        LocalDateTime startDate,
        LocalDateTime endDate,
        PromotionStatus promotionStatus,
        Long companyId
) {}
