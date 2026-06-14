package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.AccountReversalPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
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

    public CreditNoteReversalApplier(ElectronicDocumentRepository repository,
                                     AccountReversalPort accountReversalPort) {
        this.repository = repository;
        this.accountReversalPort = accountReversalPort;
    }

    /** No hace nada salvo que {@code note} sea una nota credito VALIDADA con referencia. */
    public void applyIfCreditNoteValidated(ElectronicDocument note) {
        if (note.getDocumentType() != ElectronicDocumentType.NOTA_CREDITO) return;
        if (note.getDianStatus() != DianStatus.VALIDADO) return;

        DocumentReference ref = note.getReference();
        if (ref != null && ref.cufe() != null) {
            repository.findByCufe(ref.cufe(), note.getCompanyId()).ifPresent(original -> {
                if (!original.isReversed()) {
                    original.markReversed();
                    repository.updateDianResult(original);
                }
            });
        }
        if (note.getOpenAccountId() != null) {
            accountReversalPort.markReversed(note.getOpenAccountId());
        }
    }
}
