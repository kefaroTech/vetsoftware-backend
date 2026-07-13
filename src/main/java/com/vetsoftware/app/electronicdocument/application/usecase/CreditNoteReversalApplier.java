package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.AccountReversalPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.InventoryLedgerPort;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.DocumentReference;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import org.springframework.stereotype.Component;

/**
 * Aplica el efecto contable de una nota credito VALIDADA: marca la factura referenciada como reversada
 * y propaga el reverso a la cartera (open account). Es el punto unico que subordina el void a la
 * validacion DIAN, invocado tanto por el camino sincrono (DocumentTransmitter) como por el async
 * (ProcessProviderWebhookService). Idempotente y sin control de acceso (corre dentro de la transaccion
 * del que lo llama, que ya valido ownership o es un webhook firmado).
 */
@Component
public class CreditNoteReversalApplier {
    private final ElectronicDocumentRepository repository;
    private final AccountReversalPort accountReversalPort;
    private final InventoryLedgerPort inventoryLedger;

    public CreditNoteReversalApplier(ElectronicDocumentRepository repository,
                                     AccountReversalPort accountReversalPort,
                                     InventoryLedgerPort inventoryLedger) {
        this.repository = repository;
        this.accountReversalPort = accountReversalPort;
        this.inventoryLedger = inventoryLedger;
    }

    /** No hace nada salvo que {@code note} sea una nota credito VALIDADA con referencia. */
    public void applyIfCreditNoteValidated(ElectronicDocument note) {
        if (note.getDocumentType() != ElectronicDocumentType.NOTA_CREDITO) return;
        if (note.getDianStatus() != DianStatus.VALIDADO) return;

        DocumentReference ref = note.getReference();
        if (ref == null || ref.cufe() == null) return;
        repository.findByCufe(ref.cufe(), note.getCompanyId()).ifPresent(original -> {
            // 3.9 - solo una NC TOTAL (cubre el total de la factura) anula el original y reversa la cartera.
            // Una NC PARCIAL emite el documento fiscal pero NO reversa la venta completa: evita borrar todo el
            // saldo por un crédito parcial. El ajuste parcial de la cartera queda como paso aparte.
            boolean fullCredit = note.getPayableAmount().compareTo(original.getPayableAmount()) >= 0;
            if (!fullCredit) return;
            if (!original.isReversed()) {
                original.markReversed();
                repository.updateDianResult(original);
                // POS directo (sin cuenta): repone el inventario descontado por la venta. Las ventas por cuenta
                // abierta NO se reponen aquí — su stock se repone al anular cada cargo (OPEN_ACCOUNT_CHARGE); aquí
                // solo se marca reversada la factura y su cartera. Idempotente (el ledger no re-compensa).
                if (original.getOpenAccountId() == null) {
                    inventoryLedger.reversePosSale(original.getId(), null);
                }
            }
            if (note.getOpenAccountId() != null) {
                accountReversalPort.markReversed(note.getOpenAccountId());
            }
        });
    }
}
