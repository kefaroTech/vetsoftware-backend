package com.vetsoftware.app.animal.application.dto;

import com.vetsoftware.app.animal.domain.WeightRecord;
import com.vetsoftware.app.animal.domain.WeightSource;
import com.vetsoftware.app.animal.domain.WeightType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record WeightRecordDto(
    Long id,
    Long animalId,
    String animalName,
    String animalCode,
    BigDecimal value,
    WeightType unit,
    LocalDate measuredAt,
    WeightSource source,
    Long sourceId,
    String note,
    LocalDateTime createdDate) {
  public static WeightRecordDto from(WeightRecord r) {
    return new WeightRecordDto(
        r.getId(),
        r.getAnimal().id(),
        r.getAnimal().name(),
        r.getAnimal().code(),
        r.getValue(),
        r.getUnit(),
        r.getMeasuredAt(),
        r.getSource(),
        r.getSourceId(),
        r.getNote(),
        r.getCreatedDate());
  }
}
