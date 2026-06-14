package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.IssueCreditNoteCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.IssueCreditNoteUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.DocumentAlreadyReversedException;
import com.vetsoftware.app.electronicdocument.domain.DocumentNotValidatedException;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite una nota credito total (anulacion) sobre una factura VALIDADA y la transmite por el proveedor.
 * Para proveedores sincronos (Factus) la nota queda VALIDADA aqui y el reverso de cartera se aplica en
 * el acto (DocumentTransmitter -> CreditNoteReversalApplier); para async (MATIAS) queda PENDIENTE y el
 * reverso se aplica al llegar la validacion por webhook. NUNCA se reversa la cartera antes de validar.
 */
@Observed(name = "electronicDocument.creditNote")
@Service
public class IssueCreditNoteService implements IssueCreditNoteUseCase {
    private final ElectronicDocumentRepository repository;
    private final DocumentTransmitter transmitter;

    public IssueCreditNoteService(ElectronicDocumentRepository repository, DocumentTransmitter transmitter) {
        this.repository = repository;
        this.transmitter = transmitter;
    }

    @Override
    @Transactional
    public ElectronicDocumentDto execute(IssueCreditNoteCommand command) {
        ElectronicDocument original = repository.findById(command.documentId())
                .orElseThrow(() -> new ElectronicDocumentNotFoundException(command.documentId()));
        if (!command.companyId().equals(original.getCompanyId())) {
            // No filtrar documentos de otra empresa.
            throw new ElectronicDocumentNotFoundException(command.documentId());
        }
        if (original.isNote()) {
            throw new IllegalArgumentException("No se puede emitir una nota credito sobre otra nota.");
        }
        if (original.getDianStatus() != DianStatus.VALIDADO) {
            throw new DocumentNotValidatedException(original.getId(), original.getDianStatus());
        }
        if (original.isReversed()) {
            throw new DocumentAlreadyReversedException(original.getId());
        }
        ElectronicDocument note = ElectronicDocument.createCreditNote(
                original, command.reason().dianCode(), command.reason().description());
        ElectronicDocument saved = repository.save(note);
        return ElectronicDocumentDto.from(transmitter.transmit(saved));
    }
}
