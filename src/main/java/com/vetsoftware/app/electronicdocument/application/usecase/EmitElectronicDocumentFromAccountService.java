package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.EmitElectronicDocumentFromAccountUseCase;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite end-to-end: construye el documento PENDIENTE desde la cuenta cerrada y lo transmite al proveedor.
 * Con MATIAS (asíncrono) el documento queda PENDIENTE y la entrega de la representación la dispara el
 * cierre async (webhook o polling de estado) al validar. (Un proveedor síncrono entregaría en el acto.)
 */
@Observed(name = "electronicDocument.emit")
@Service
public class EmitElectronicDocumentFromAccountService implements EmitElectronicDocumentFromAccountUseCase {
    private final DocumentBuilder documentBuilder;
    private final DocumentTransmitter documentTransmitter;
    private final DeliverElectronicDocumentService deliverService;

    public EmitElectronicDocumentFromAccountService(DocumentBuilder documentBuilder,
                                                    DocumentTransmitter documentTransmitter,
                                                    DeliverElectronicDocumentService deliverService) {
        this.documentBuilder = documentBuilder;
        this.documentTransmitter = documentTransmitter;
        this.deliverService = deliverService;
    }

    @Override
    @Transactional
    public ElectronicDocumentDto execute(EmitElectronicDocumentCommand command) {
        ElectronicDocument document = documentBuilder.build(
                command.openAccountId(), command.documentType(), command.companyId(),
                command.finalConsumer());
        ElectronicDocument transmitted = documentTransmitter.transmit(document);
        deliverService.deliverIfValidated(transmitted);
        return ElectronicDocumentDto.from(transmitted);
    }
}
