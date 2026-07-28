package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.IssueDebitNoteCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.IssueDebitNoteUseCase;
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
 * Emite una nota debito (aumento) sobre una factura VALIDADA y la transmite. A diferencia de la nota
 * credito, NO reversa la cartera: solo aumenta el valor del documento referenciado.
 */
@Observed(name = "electronic.document.debit.note")
@Service
public class IssueDebitNoteService implements IssueDebitNoteUseCase {
    private final ElectronicDocumentRepository repository;
    private final ElectronicDocumentEmitter emitter;

    public IssueDebitNoteService(ElectronicDocumentRepository repository, ElectronicDocumentEmitter emitter) {
        this.repository = repository;
        this.emitter = emitter;
    }

    @Override
    @Transactional
    public ElectronicDocumentDto execute(IssueDebitNoteCommand command) {
        ElectronicDocument original = repository.findById(command.documentId())
                .orElseThrow(() -> new ElectronicDocumentNotFoundException(command.documentId()));
        if (!command.companyId().equals(original.getCompanyId())) {
            throw new ElectronicDocumentNotFoundException(command.documentId());
        }
        if (original.isNote()) {
            throw new IllegalArgumentException("No se puede emitir una nota debito sobre otra nota.");
        }
        if (original.getDianStatus() != DianStatus.VALIDADO) {
            throw new DocumentNotValidatedException(original.getId(), original.getDianStatus());
        }
        if (original.isReversed()) {
            throw new DocumentAlreadyReversedException(original.getId());
        }
        ElectronicDocument note = ElectronicDocument.createDebitNote(
                original, command.reason().dianCode(), command.reason().description(),
                command.issuedByEmployeeId(), command.additionalAmount());
        // Persiste la nota PENDIENTE; el emisor numera+transmite (empresa con BILLING) o la guarda local.
        ElectronicDocument saved = repository.save(note);
        return ElectronicDocumentDto.from(emitter.emit(saved));
    }
}
