package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.EmitElectronicDocumentOnCloseUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite el documento electrónico al cerrar/cobrar una cuenta. Reusa el mismo núcleo que la emisión manual
 * (construir desde la cuenta cerrada + {@link ElectronicDocumentEmitter} + entrega): con plan premium
 * (submódulo BILLING) numera y transmite a la DIAN; sin él, el emisor lo guarda local (NO_ELECTRONICO).
 * Idempotente: si la cuenta ya tiene documento, no emite otro (evita duplicar el documento fiscal).
 */
@Observed(name = "electronicDocument.emitOnClose")
@Service
public class EmitElectronicDocumentOnCloseService implements EmitElectronicDocumentOnCloseUseCase {
    private final DocumentBuilder documentBuilder;
    private final ElectronicDocumentEmitter emitter;
    private final DeliverElectronicDocumentService deliverService;
    private final ElectronicDocumentRepository repository;

    public EmitElectronicDocumentOnCloseService(DocumentBuilder documentBuilder,
                                                ElectronicDocumentEmitter emitter,
                                                DeliverElectronicDocumentService deliverService,
                                                ElectronicDocumentRepository repository) {
        this.documentBuilder = documentBuilder;
        this.emitter = emitter;
        this.deliverService = deliverService;
        this.repository = repository;
    }

    @Override
    @Transactional
    public ElectronicDocumentDto execute(EmitElectronicDocumentCommand command) {
        if (repository.existsByOpenAccountId(command.openAccountId())) {
            return null; // la cuenta ya fue facturada: idempotencia, no duplicar
        }
        ElectronicDocument document = documentBuilder.build(
                command.openAccountId(), command.documentType(), command.companyId(),
                command.finalConsumer());
        ElectronicDocument emitted = emitter.emit(document);
        deliverService.deliverIfValidated(emitted);
        return ElectronicDocumentDto.from(emitted);
    }
}
