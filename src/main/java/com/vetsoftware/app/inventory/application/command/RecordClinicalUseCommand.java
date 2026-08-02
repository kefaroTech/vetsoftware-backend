package com.vetsoftware.app.inventory.application.command;

/**
 * Consumo clínico de un producto (aplicado a un paciente). Salida FEFO tipo
 * CLINICAL_USE, referencia CLINICAL_EVENT. {@code referenceId} = id del evento
 * clínico si se dispara automáticamente; null en el consumo manual. Respeta el
 * flag de stock negativo de la empresa (por defecto bloquea sin existencias).
 */
public record RecordClinicalUseCommand(Long companyId, Long branchId, Long productId, int quantity,
        Long referenceId, String reason, Long createdBy) {
}
