package com.vetsoftware.app.promotion.infrastructure.web.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreatePromotionRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank String promotionType,
    @NotBlank String applicationType,
    @NotNull @Positive Long applicationItem,
    @NotBlank String valueType,
    @NotNull @DecimalMin("0.0") BigDecimal value,
    @NotNull LocalDateTime startDate,
    @NotNull LocalDateTime endDate,
    @NotBlank String promotionStatus) {}
