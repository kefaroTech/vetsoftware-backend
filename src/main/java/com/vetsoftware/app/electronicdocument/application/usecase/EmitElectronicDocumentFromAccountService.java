package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.EmitElectronicDocumentFromAccountUseCase;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite end-to-end: construye el documento PENDIENTE desde la cuenta cerrada, lo transmite al proveedor
 * y, si queda VALIDADO (síncrono, p. ej. Factus), genera y envía la representación. Si el proveedor es
 * asíncrono (MATIAS), el documento queda PENDIENTE y la entrega la dispara el webhook al validar.
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
                command.openAccountId(), command.documentType(), command.companyId());
        ElectronicDocument transmitted = documentTransmitter.transmit(document);
        deliverService.deliverIfValidated(transmitted);
        return ElectronicDocumentDto.from(transmitted);
    }
}
