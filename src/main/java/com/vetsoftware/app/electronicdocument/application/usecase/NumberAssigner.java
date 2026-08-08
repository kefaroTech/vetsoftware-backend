package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.NumberingAllocationPort;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Asigna la numeración fiscal a un documento PENDIENTE desde la
 * {@code NumberingResolution} activa de la empresa (consecutivo continuo,
 * atómico). Reutilizable por la emisión de facturas/POS y por las notas. Sin
 * control de acceso (lo invocan casos de uso ya autorizados).
 *
 * <p>
 * <b>Transacción propia y corta ({@code REQUIRES_NEW}).</b> El adaptador toma
 * un {@code SELECT … FOR UPDATE} sobre la resolución para serializar emisiones
 * concurrentes, y ese lock solo existe mientras haya una transacción abierta.
 * Los casos de uso de emisión NO son transaccionales —a propósito: llaman
 * después a {@link DocumentTransmitter}, que hace hasta 75 segundos de HTTP—,
 * así que sin esta anotación el {@code FOR UPDATE} se liberaba al terminar la
 * consulta y dos emisiones simultáneas podían llevarse el mismo consecutivo.
 * Con {@code REQUIRES_NEW} el lock cubre leer-incrementar-guardar y se suelta
 * en milisegundos, mucho antes de que empiece la llamada al proveedor.
 */
@Component
public class NumberAssigner {
    private final NumberingAllocationPort numberingPort;

    public NumberAssigner(NumberingAllocationPort numberingPort) {
        this.numberingPort = numberingPort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void assign(ElectronicDocument document) {
        NumberingAllocationPort.AllocatedNumber number = numberingPort
                .allocate(document.getCompanyId(), document.getBranchId(),
                        document.getDocumentType())
                .orElseThrow(() -> new IllegalStateException(
                        "La empresa no tiene una resolución de numeración activa para "
                                + document.getDocumentType() + "."));
        document.assignNumber(number.resolutionNumber(), number.prefix(), number.consecutive());
    }

    /**
     * Asigna SOLO resolución + prefijo (sin consumir consecutivo) para documentos
     * cuyo número lo asigna el proveedor DIAN (POS auto-increment). El proveedor
     * exige igual resolución+prefijo en el request.
     */
    public void assignResolutionOnly(ElectronicDocument document) {
        NumberingAllocationPort.AllocatedNumber number = numberingPort
                .peekActive(document.getCompanyId(), document.getBranchId(),
                        document.getDocumentType())
                .orElseThrow(() -> new IllegalStateException(
                        "La empresa no tiene una resolución de numeración activa para "
                                + document.getDocumentType() + "."));
        document.assignResolutionOnly(number.resolutionNumber(), number.prefix());
    }

    /**
     * Ante un rechazo, intenta recuperar el consecutivo del documento para evitar
     * un hueco en la secuencia fiscal. Si la resolución pudo recuperarlo (era el
     * último entregado), limpia la numeración del documento para que no queden dos
     * filas con el mismo número. Si no era seguro, no toca nada (el hueco
     * permanece). No-op si el documento nunca llegó a numerarse.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(ElectronicDocument document) {
        if (document.getConsecutive() == null)
            return;
        boolean reclaimed = numberingPort.release(document.getCompanyId(), document.getBranchId(),
                document.getDocumentType(), document.getConsecutive());
        if (reclaimed)
            document.releaseFiscalNumber();
    }
}
