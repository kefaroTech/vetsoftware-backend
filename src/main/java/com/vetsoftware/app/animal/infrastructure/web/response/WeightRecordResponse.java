package com.vetsoftware.app.animal.infrastructure.web.response;

import com.vetsoftware.app.animal.domain.WeightSource;
import com.vetsoftware.app.animal.domain.WeightType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WeightRecordResponse(Long id, Long animalId, String animalName, String animalCode,
        BigDecimal value, WeightType unit, LocalDate measuredAt, WeightSource source, Long sourceId,
        String note, LocalDateTime createdDate) {
}
