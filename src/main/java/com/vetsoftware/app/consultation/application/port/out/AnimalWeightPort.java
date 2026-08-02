package com.vetsoftware.app.consultation.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Puerto para registrar el peso capturado durante una consulta como punto de la
 * serie temporal del animal (feature animal). Se comunica por primitivos: la
 * unidad viaja como String y el paquete animal resuelve el enum, respetando el
 * vertical slicing.
 */
public interface AnimalWeightPort {
    void recordConsultationWeight(Long animalId, Long companyId, BigDecimal value, String unit,
            LocalDate measuredAt, Long consultationId);
}
