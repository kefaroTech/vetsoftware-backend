package com.vetsoftware.app.companytrialgrant.domain;

/**
 * Ese artículo ya se le regaló a esa empresa.
 *
 * <p>
 * <strong>Y no se le regala dos veces, jamás.</strong> La unicidad
 * {@code (company_id, catalog_item_id)} lo impone en el motor y el código no
 * intenta sortearla: esta excepción existe para que el operador lea qué pasó,
 * no para abrir una vía alternativa. Reponer un módulo quitado reusa la
 * concesión que ya existe —con los días que le quedaban—, no crea otra.
 */
public class TrialAlreadyGrantedException extends RuntimeException {

    public TrialAlreadyGrantedException(Long companyId, Long catalogItemId) {
        super("Company " + companyId + " already used its trial of catalog item " + catalogItemId
                + ": an item is never granted twice to the same company");
    }
}
