package com.vetsoftware.app.supplierwithholding.application.port.in;

import com.vetsoftware.app.supplierwithholding.application.command.RegisterSupplierWithholdingPaymentCommand;
import com.vetsoftware.app.supplierwithholding.application.dto.SupplierWithholdingDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface RegisterSupplierWithholdingPaymentUseCase {

    /**
     * Anota la prueba de la consignacion de lo retenido. Conservarla es obligacion
     * expresa del art. 632 ET: sin ella no se puede probar que lo retenido se
     * consigno.
     */
    @PreAuthorize("hasRole('SYSTEM')")
    SupplierWithholdingDto execute(RegisterSupplierWithholdingPaymentCommand command);
}
