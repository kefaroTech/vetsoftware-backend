package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.NumberingAllocationPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import org.springframework.stereotype.Component;

/**
 * Asigna la numeración fiscal a un documento PENDIENTE desde la {@code NumberingResolution} activa de la
 * empresa (consecutivo continuo, atómico). Reutilizable por la emisión de facturas/POS y por las notas.
 * Sin control de acceso (lo invocan casos de uso ya autorizados).
 */
@Component
public class NumberAssigner {
    private final NumberingAllocationPort numberingPort;

    public NumberAssigner(NumberingAllocationPort numberingPort) {
        this.numberingPort = numberingPort;
    }

    public void assign(ElectronicDocument document) {
        NumberingAllocationPort.AllocatedNumber number = numberingPort
                .allocate(document.getCompanyId(), document.getDocumentType())
                .orElseThrow(() -> new IllegalStateException(
                        "La empresa no tiene una resolución de numeración activa para "
                                + document.getDocumentType() + "."));
        document.assignNumber(number.resolutionNumber(), number.prefix(), number.consecutive());
    }
}
