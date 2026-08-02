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
 * Emite una nota credito total (anulacion) sobre una factura VALIDADA y la transmite por el
 * proveedor. Con MATIAS (async) la nota queda PENDIENTE y el reverso de cartera se aplica al llegar
 * la validacion (por webhook o por polling de estado) via CreditNoteReversalApplier. NUNCA se
 * reversa antes de validar.
 */
@Observed(name = "electronic.document.credit.note")
@Service
public class IssueCreditNoteService implements IssueCreditNoteUseCase {
  private final ElectronicDocumentRepository repository;
  private final ElectronicDocumentEmitter emitter;

  public IssueCreditNoteService(
      ElectronicDocumentRepository repository, ElectronicDocumentEmitter emitter) {
    this.repository = repository;
    this.emitter = emitter;
  }

  @Override
  @Transactional
  public ElectronicDocumentDto execute(IssueCreditNoteCommand command) {
    ElectronicDocument original =
        repository
            .findById(command.documentId())
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
    ElectronicDocument note =
        ElectronicDocument.createCreditNote(
            original,
            command.reason().dianCode(),
            command.reason().description(),
            command.issuedByEmployeeId(),
            command.partialAmount());
    // Persiste la nota PENDIENTE; el emisor numera+transmite (empresa con BILLING) o la guarda
    // local.
    ElectronicDocument saved = repository.save(note);
    return ElectronicDocumentDto.from(emitter.emit(saved));
  }
}
