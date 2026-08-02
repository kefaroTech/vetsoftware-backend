package com.vetsoftware.app.hospitalization.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Puerto para registrar el peso capturado al ingresar una hospitalización como punto de la serie
 * temporal del animal (feature animal). La unidad viaja como String; el paquete animal resuelve el
 * enum, respetando el vertical slicing.
 */
public interface AnimalWeightPort {
  void recordHospitalizationWeight(
      Long animalId,
      Long companyId,
      BigDecimal value,
      String unit,
      LocalDate measuredAt,
      Long hospitalizationId);
}
