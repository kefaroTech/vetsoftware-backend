package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.RegisterPosSaleUseCase;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra y emite una venta de POS: construye el documento PENDIENTE desde el payload y lo pasa por el
 * emisor (mismo gate BILLING que el cierre de cuenta). Con BILLING: numera + transmite. Sin BILLING: queda
 * PENDIENTE (datos guardados, emision a la DIAN diferida y re-emitible al habilitar el modulo).
 */
@Observed(name = "electronicDocument.posSale")
@Service
public class RegisterPosSaleService implements RegisterPosSaleUseCase {
    private final PosSaleDocumentBuilder documentBuilder;
    private final ElectronicDocumentEmitter emitter;
    private final DeliverElectronicDocumentService deliverService;

    public RegisterPosSaleService(PosSaleDocumentBuilder documentBuilder,
                                  ElectronicDocumentEmitter emitter,
                                  DeliverElectronicDocumentService deliverService) {
        this.documentBuilder = documentBuilder;
        this.emitter = emitter;
        this.deliverService = deliverService;
    }

    @Override
    @Transactional
    public ElectronicDocumentDto execute(RegisterPosSaleCommand command) {
        ElectronicDocument document = documentBuilder.build(command);
        ElectronicDocument emitted = emitter.emit(document);
        deliverService.deliverIfValidated(emitted);
        return ElectronicDocumentDto.from(emitted);
    }
}
