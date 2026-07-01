package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.RegisterPosSaleCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.RegisterPosSaleUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import io.micrometer.observation.annotation.Observed;
import java.util.Optional;
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
    private final ElectronicDocumentRepository repository;

    public RegisterPosSaleService(PosSaleDocumentBuilder documentBuilder,
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
    public ElectronicDocumentDto execute(RegisterPosSaleCommand command) {
        // Idempotencia: si el POST se reintenta con la misma key (respuesta perdida en transporte), se
        // devuelve el documento ya emitido en vez de registrar y transmitir OTRA venta a la DIAN. El índice
        // único (company_id, client_request_id) respalda la carrera concurrente (la 2ª inserción la rechaza la BD).
        if (command.clientRequestId() != null && !command.clientRequestId().isBlank()) {
            Optional<ElectronicDocument> existing =
                    repository.findByCompanyIdAndClientRequestId(command.companyId(), command.clientRequestId());
            if (existing.isPresent()) {
                return ElectronicDocumentDto.from(existing.get());
            }
        }
        ElectronicDocument document = documentBuilder.build(command);
        ElectronicDocument emitted = emitter.emit(document);
        deliverService.deliverIfValidated(emitted);
        return ElectronicDocumentDto.from(emitted);
    }
}
