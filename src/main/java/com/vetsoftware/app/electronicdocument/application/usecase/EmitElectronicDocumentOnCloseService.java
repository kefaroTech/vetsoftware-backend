package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.EmitElectronicDocumentOnCloseUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Emite el documento electrónico al cerrar/cobrar una cuenta. Construye desde la cuenta cerrada el documento
 * PENDIENTE (dentro de la transacción del cierre) y difiere la transmisión + entrega a {@link
 * ClosedAccountEmissionCompleter} tras el commit (A1: el I/O externo NO corre bajo el lock pesimista de la
 * cuenta). Idempotente: si la cuenta ya tiene documento, no emite otro (evita duplicar el documento fiscal).
 */
@Observed(name = "electronic.document.emit.on.close")
@Service
public class EmitElectronicDocumentOnCloseService implements EmitElectronicDocumentOnCloseUseCase {
    private static final Logger log = LoggerFactory.getLogger(EmitElectronicDocumentOnCloseService.class);

    private final DocumentBuilder documentBuilder;
    private final ClosedAccountEmissionCompleter emissionCompleter;
    private final ElectronicDocumentRepository repository;

    public EmitElectronicDocumentOnCloseService(DocumentBuilder documentBuilder,
                                                ClosedAccountEmissionCompleter emissionCompleter,
                                                ElectronicDocumentRepository repository) {
        this.documentBuilder = documentBuilder;
        this.emissionCompleter = emissionCompleter;
        this.repository = repository;
    }

    @Override
    @Transactional
    public ElectronicDocumentDto execute(EmitElectronicDocumentCommand command) {
        if (repository.existsByOpenAccountId(command.openAccountId())) {
            return null; // la cuenta ya fue facturada: idempotencia, no duplicar
        }
        // Bajo el lock pesimista del cierre: SOLO se persiste el documento PENDIENTE. Un fallo de configuración
        // que valida el builder (p. ej. sin perfil fiscal) sigue haciendo fallar el cierre atómicamente.
        ElectronicDocument document = documentBuilder.build(
                command.openAccountId(), command.documentType(), command.companyId(),
                command.finalConsumer());
        // A1: la transmisión a MATIAS + entrega (PDF/S3/email) se difiere a DESPUÉS del commit del cierre, en su
        // propia transacción, para no retener el lock pesimista de la cuenta ni la conexión del pool durante el
        // I/O externo (~60s). Si falla, el documento queda PENDIENTE (re-emitible), sin revertir el cierre.
        Long documentId = document.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emissionCompleter.complete(documentId);
                } catch (Exception e) {
                    log.error("Emisión DIAN post-cierre falló para el documento {} "
                            + "(queda PENDIENTE, re-emitible): {}", documentId, e.getMessage(), e);
                }
            }
        });
        return ElectronicDocumentDto.from(document);
    }
}
