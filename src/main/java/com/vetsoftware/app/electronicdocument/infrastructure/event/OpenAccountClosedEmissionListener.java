package com.vetsoftware.app.electronicdocument.infrastructure.event;

import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.port.in.EmitElectronicDocumentOnCloseUseCase;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.openaccount.application.event.OpenAccountClosedForEmissionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dispara la facturación electrónica cuando una cuenta se cierra/cobra (evento publicado por la feature
 * openaccount). Corre {@code AFTER_COMMIT} para que la emisión vea la cuenta ya CLOSE y en su propia
 * transacción. Es **best-effort**: cualquier fallo (DIAN caída, sin numeración/proveedor) se registra y se
 * traga — NUNCA bloquea ni revierte la venta; el documento queda para reintento/contingencia.
 *
 * Este listener (infraestructura de electronicdocument) es el único punto que conoce el evento de
 * openaccount; mantiene la dirección de dependencia existente (electronicdocument → openaccount) y evita
 * un ciclo entre features.
 */
@Component
public class OpenAccountClosedEmissionListener {
    private static final Logger log = LoggerFactory.getLogger(OpenAccountClosedEmissionListener.class);

    private final EmitElectronicDocumentOnCloseUseCase emitOnClose;

    public OpenAccountClosedEmissionListener(EmitElectronicDocumentOnCloseUseCase emitOnClose) {
        this.emitOnClose = emitOnClose;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAccountClosed(OpenAccountClosedForEmissionEvent event) {
        try {
            emitOnClose.execute(new EmitElectronicDocumentCommand(
                    event.openAccountId(),
                    parseType(event.documentType()),
                    event.companyId(),
                    event.finalConsumer()));
        } catch (Exception e) {
            log.warn("Auto-emisión tras cierre de la cuenta {} falló (no bloquea la venta): {}",
                    event.openAccountId(), e.getMessage());
        }
    }

    /** Solo FE_VENTA o DOC_EQUIV_POS son válidos al cerrar; cualquier otro valor cae a documento POS. */
    private ElectronicDocumentType parseType(String raw) {
        if (raw != null) {
            try {
                ElectronicDocumentType type = ElectronicDocumentType.valueOf(raw);
                if (type == ElectronicDocumentType.FE_VENTA || type == ElectronicDocumentType.DOC_EQUIV_POS) {
                    return type;
                }
            } catch (IllegalArgumentException ignored) {
                // valor desconocido → default
            }
        }
        return ElectronicDocumentType.DOC_EQUIV_POS;
    }
}
