package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.EmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.port.in.EmitElectronicDocumentOnCloseUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Emite el documento electrónico al cerrar/cobrar una cuenta. Construye desde
 * la cuenta cerrada el documento PENDIENTE (dentro de la transacción del
 * cierre) y difiere la transmisión + entrega a
 * {@link ClosedAccountEmissionCompleter} tras el commit (A1: el I/O externo NO
 * corre bajo el lock pesimista de la cuenta). Idempotente: si la cuenta ya
 * tiene documento, no emite otro (evita duplicar el documento fiscal).
 */
@Observed(name = "electronic.document.emit.on.close")
@Service
public class EmitElectronicDocumentOnCloseService implements EmitElectronicDocumentOnCloseUseCase {
    private static final Logger log = LoggerFactory
            .getLogger(EmitElectronicDocumentOnCloseService.class);

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
        // Bajo el lock pesimista del cierre: SOLO se persiste el documento PENDIENTE.
        // Un fallo de
        // configuración
        // que valida el builder (p. ej. sin perfil fiscal) sigue haciendo fallar el
        // cierre
        // atómicamente.
        ElectronicDocument document = documentBuilder.build(command.openAccountId(),
                command.documentType(), command.companyId(), command.finalConsumer());
        // A1: la transmisión a MATIAS + entrega (PDF/S3/email) se difiere a DESPUÉS del
        // commit del
        // cierre, en su
        // propia transacción, para no retener el lock pesimista de la cuenta ni la
        // conexión del pool
        // durante el
        // I/O externo (~60s). Si falla, el documento queda PENDIENTE (re-emitible), sin
        // revertir el
        // cierre.
        Long documentId = document.getId();
        // La empresa se resuelve AQUI, dentro de la transaccion: despues del commit
        // la conexion volvio al pool y el completer necesita el companyId para
        // releer el documento con el finder acotado.
        Long companyId = command.companyId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emissionCompleter.complete(documentId, companyId);
                } catch (Exception e) {
                    log.error("Emisión DIAN post-cierre falló para el documento {} — {}: {}",
                            documentId, recoveryOutcomeOf(documentId, companyId), e.getMessage(),
                            e);
                }
            }
        });
        return ElectronicDocumentDto.from(document);
    }

    /**
     * Describe la recuperación posible a partir del estado REAL del documento, no
     * de una constante. El mensaje anterior prometía «queda PENDIENTE, re-emitible»
     * pasara lo que pasara, y eso solo es cierto cuando la emisión falla antes de
     * que la DIAN se pronuncie. Si la transmisión llegó a VALIDADO y lo que reventó
     * fue el QR, el render del PDF o S3, el documento NO es re-emitible: la
     * reconciliación solo arrienda PENDIENTE y {@code POST /{id}/transmit} retorna
     * sin hacer nada sobre un estado terminal, de modo que nadie vuelve a intentar
     * la entrega. Prometer lo contrario en el log hace que la guardia archive el
     * incidente como auto-recuperable y el documento se quede validado y sin
     * representación gráfica.
     *
     * <p>
     * La relectura es una consulta extra que solo ocurre en la rama de error, y va
     * blindada: esto corre dentro de un {@code afterCommit} y una excepción
     * escapando de aquí taparía el error original que se está intentando registrar.
     */
    private String recoveryOutcomeOf(Long documentId, Long companyId) {
        DianStatus status;
        try {
            status = repository.findByIdAndCompanyId(documentId, companyId)
                    .map(ElectronicDocument::getDianStatus).orElse(null);
        } catch (Exception relectura) {
            return "no se pudo releer su estado, se desconoce si es re-emitible";
        }
        if (status == null) {
            return "no se pudo determinar su estado, se desconoce si es re-emitible";
        }
        if (status == DianStatus.PENDIENTE) {
            return "queda PENDIENTE y es re-emitible por POST /{id}/transmit"
                    + " o por la reconciliación";
        }
        return "quedó en " + status + " y NO es re-emitible: la reconciliación y"
                + " POST /{id}/transmit solo actúan sobre PENDIENTE, requiere revisión manual";
    }
}
