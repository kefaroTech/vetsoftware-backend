package com.vetsoftware.app.clinicalhistory.application.dto;

import java.util.List;

/**
 * Detalle clínico rico de un evento: título legible + campos (formato SOAP) +
 * tablas (medicamentos, medicaciones/procedimientos de hospitalización),
 * cargado desde la entidad fuente para el reporte PDF.
 */
public record ClinicalEventDetail(String title, List<DetailField> fields,
        List<DetailTable> tables) {

    /**
     * Detalle sin tablas (eventos cuyo contenido cabe en campos etiqueta→valor).
     */
    public static ClinicalEventDetail of(String title, List<DetailField> fields) {
        return new ClinicalEventDetail(title, fields, List.of());
    }
}
