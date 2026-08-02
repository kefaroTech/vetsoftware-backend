package com.vetsoftware.app.electronicdocument.infrastructure.orchestration;

import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.port.in.EmitElectronicDocumentOnCloseUseCase;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.openaccount.application.port.out.ClosedAccountEmissionPort;
import org.springframework.stereotype.Component;

/**
 * Único punto que conecta el cierre de cuenta (openaccount) con la emisión del
 * documento electrónico. Implementa el puerto de salida de openaccount llamando
 * directamente al caso de uso de esta feature, así la dependencia va
 * electronicdocument → openaccount (no al revés) y no hay ciclo entre features.
 */
@Component
public class ClosedAccountEmissionAdapter implements ClosedAccountEmissionPort {
    private final EmitElectronicDocumentOnCloseUseCase emitOnClose;

    public ClosedAccountEmissionAdapter(EmitElectronicDocumentOnCloseUseCase emitOnClose) {
        this.emitOnClose = emitOnClose;
    }

    @Override
    public void emitForClosedAccount(Long openAccountId, Long companyId, String documentType,
            boolean finalConsumer) {
        emitOnClose.execute(new EmitElectronicDocumentCommand(openAccountId,
                parseType(documentType), companyId, finalConsumer));
    }

    /**
     * Solo FE_VENTA o DOC_EQUIV_POS son válidos al cerrar; cualquier otro valor cae
     * a documento POS.
     */
    private ElectronicDocumentType parseType(String raw) {
        if (raw != null) {
            try {
                ElectronicDocumentType type = ElectronicDocumentType.valueOf(raw);
                if (type == ElectronicDocumentType.FE_VENTA
                        || type == ElectronicDocumentType.DOC_EQUIV_POS) {
                    return type;
                }
            } catch (IllegalArgumentException ignored) {
                // valor desconocido → default
            }
        }
        return ElectronicDocumentType.DOC_EQUIV_POS;
    }
}
