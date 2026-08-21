package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.TransmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.TransmitElectronicDocumentUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Transmite un documento a la DIAN a través del proveedor configurado para la
 * empresa. Valida ownership y delega el envío en {@link DocumentTransmitter}.
 * Con MATIAS (async) el documento queda PENDIENTE tras transmitir; lo cierra el
 * webhook o el polling de estado. (Un proveedor síncrono cerraría aquí.)
 *
 * <p>
 * <b>Un documento ya VALIDADO no se retransmite: se re-entrega (issue
 * #204).</b> Antes, este caso de uso llamaba a {@code transmitter.transmit}
 * pasara lo que pasara, y {@code transmitInternal} no mira el estado DIAN:
 * pedir {@code POST /{id}/transmit} sobre una factura ya validada la mandaba
 * otra vez al proveedor, y con uno que no deduplique eso son dos documentos
 * fiscales donde debía haber uno. Ese camino queda cerrado. Lo que sí puede
 * faltarle a un VALIDADO es la representación gráfica, y eso es entrega, no
 * emisión: para eso el endpoint delega en
 * {@link DeliverElectronicDocumentService}, que es idempotente y no hace nada
 * si el PDF ya está.
 */
@Observed(name = "electronic.document.transmit")
@Service
public class TransmitElectronicDocumentService implements TransmitElectronicDocumentUseCase {
    private final ElectronicDocumentRepository repository;
    private final DocumentTransmitter transmitter;
    private final DeliverElectronicDocumentService deliverService;

    public TransmitElectronicDocumentService(ElectronicDocumentRepository repository,
            DocumentTransmitter transmitter, DeliverElectronicDocumentService deliverService) {
        this.repository = repository;
        this.transmitter = transmitter;
        this.deliverService = deliverService;
    }

    /**
     * Sin {@code @Transactional}: aca solo se lee el documento y se retransmite. El
     * HTTP al proveedor —hasta 75 segundos— no puede correr dentro de una
     * transaccion, y el desenlace lo guarda el transmisor en la suya. La entrega
     * tampoco puede ir en transaccion: sube a S3 y manda correo.
     */
    @Override
    public ElectronicDocumentDto execute(TransmitElectronicDocumentCommand command) {
        // El filtro por empresa va EN la consulta, no en un if posterior: el
        // companyId lo inyecta el controller desde el principal
        // (authz.currentCompanyId(), nunca null). No filtrar documentos de otra
        // empresa.
        ElectronicDocument document = repository
                .findByIdAndCompanyId(command.documentId(), command.companyId())
                .orElseThrow(() -> new ElectronicDocumentNotFoundException(command.documentId()));
        if (document.getDianStatus() == DianStatus.VALIDADO) {
            // Idempotente por partida doble: con PDF presente el guard de
            // deliverIfValidated sale sin tocar nada, y sin PDF esta es la unica via
            // manual para rescatar un documento que el job aun no alcanzo.
            deliverService.deliverIfValidated(document);
            return ElectronicDocumentDto.from(document);
        }
        return ElectronicDocumentDto.from(transmitter.transmit(document));
    }
}
