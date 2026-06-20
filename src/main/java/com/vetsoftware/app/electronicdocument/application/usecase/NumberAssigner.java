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

    /**
     * Ante un rechazo, intenta recuperar el consecutivo del documento para evitar un hueco en la secuencia
     * fiscal. Si la resolución pudo recuperarlo (era el último entregado), limpia la numeración del
     * documento para que no queden dos filas con el mismo número. Si no era seguro, no toca nada (el hueco
     * permanece). No-op si el documento nunca llegó a numerarse.
     */
    public void release(ElectronicDocument document) {
        if (document.getConsecutive() == null) return;
        boolean reclaimed = numberingPort.release(
                document.getCompanyId(), document.getDocumentType(), document.getConsecutive());
        if (reclaimed) document.releaseFiscalNumber();
    }
}
