package com.vetsoftware.app.supplierwithholding.application.port.in;

import com.vetsoftware.app.supplierwithholding.application.command.PracticeSupplierWithholdingCommand;
import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface PracticeSupplierWithholdingUseCase {

    /**
     * Registra una retencion practicada a un proveedor.
     *
     * <p>
     * <strong>{@code hasRole('SYSTEM')} a secas.</strong> Es una obligacion fiscal
     * de VetSoftware como agente de retencion: cero superficie de cliente, ninguna
     * empresa a la que acotar. Toda la feature comparte ese gate
     * ({@code GATE_COHERENTE_EN_FEATURE_DE_SYSTEM}).
     */
    @PreAuthorize("hasRole('SYSTEM')")
    SupplierWithholdingDto execute(PracticeSupplierWithholdingCommand command);
}
