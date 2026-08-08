package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.TransmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.TransmitElectronicDocumentUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

/**
 * Transmite un documento a la DIAN a través del proveedor configurado para la
 * empresa. Valida ownership y delega el envío en {@link DocumentTransmitter}.
 * Con MATIAS (async) el documento queda PENDIENTE tras transmitir; lo cierra el
 * webhook o el polling de estado. (Un proveedor síncrono cerraría aquí.)
 */
@Observed(name = "electronic.document.transmit")
@Service
public class TransmitElectronicDocumentService implements TransmitElectronicDocumentUseCase {
    private final ElectronicDocumentRepository repository;
    private final DocumentTransmitter transmitter;

    public TransmitElectronicDocumentService(ElectronicDocumentRepository repository,
            DocumentTransmitter transmitter) {
        this.repository = repository;
        this.transmitter = transmitter;
    }

    /**
     * Sin {@code @Transactional}: aca solo se lee el documento y se retransmite. El
     * HTTP al proveedor —hasta 75 segundos— no puede correr dentro de una
     * transaccion, y el desenlace lo guarda el transmisor en la suya.
     */
    @Override
    public ElectronicDocumentDto execute(TransmitElectronicDocumentCommand command) {
        ElectronicDocument document = repository.findById(command.documentId())
                .orElseThrow(() -> new ElectronicDocumentNotFoundException(command.documentId()));
        if (!command.companyId().equals(document.getCompanyId())) {
            // No filtrar documentos de otra empresa.
            throw new ElectronicDocumentNotFoundException(command.documentId());
        }
        return ElectronicDocumentDto.from(transmitter.transmit(document));
    }
}
