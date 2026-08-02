package com.vetsoftware.app.promotion.infrastructure.web.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionResponse(
    Long id,
    String name,
    String promotionType,
    String applicationType,
    Long applicationItem,
    String valueType,
    BigDecimal value,
    LocalDateTime startDate,
    LocalDateTime endDate,
    String promotionStatus,
    CompanySummary company,
    LocalDateTime createdDate,
    boolean enabled) {}
