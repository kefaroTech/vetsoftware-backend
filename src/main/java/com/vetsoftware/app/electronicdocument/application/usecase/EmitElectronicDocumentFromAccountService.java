package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.EmitElectronicDocumentFromAccountUseCase;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite end-to-end: construye el documento PENDIENTE desde la cuenta cerrada y
 * lo transmite al proveedor. Con MATIAS (asíncrono) el documento queda
 * PENDIENTE y la entrega de la representación la dispara el cierre async
 * (webhook o polling de estado) al validar. (Un proveedor síncrono entregaría
 * en el acto.)
 */
@Observed(name = "electronic.document.emit")
@Service
public class EmitElectronicDocumentFromAccountService
        implements
            EmitElectronicDocumentFromAccountUseCase {
    private final DocumentBuilder documentBuilder;
    private final ElectronicDocumentEmitter emitter;
    private final DeliverElectronicDocumentService deliverService;

    public EmitElectronicDocumentFromAccountService(DocumentBuilder documentBuilder,
            ElectronicDocumentEmitter emitter, DeliverElectronicDocumentService deliverService) {
        this.documentBuilder = documentBuilder;
        this.emitter = emitter;
        this.deliverService = deliverService;
    }

    @Override
    @Transactional
    public ElectronicDocumentDto execute(EmitElectronicDocumentCommand command) {
        ElectronicDocument document = documentBuilder.build(command.openAccountId(),
                command.documentType(), command.companyId(), command.finalConsumer());
        // Si la empresa tiene BILLING: numera (justo antes de transmitir) y transmite.
        // Si no: el emisor
        // lo
        // marca NO_ELECTRONICO y lo guarda localmente, sin numeración ni MATIAS.
        ElectronicDocument emitted = emitter.emit(document);
        deliverService.deliverIfValidated(emitted);
        return ElectronicDocumentDto.from(emitted);
    }
}
