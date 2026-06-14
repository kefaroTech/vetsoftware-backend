package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicInvoiceProviderPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigSnapshot;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderResult;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.TransmissionResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Núcleo de transmisión a la DIAN, reutilizable y SIN control de acceso (lo invocan tanto el caso de
 * uso con `@PreAuthorize` como el job de contingencia, que corre sin contexto de seguridad). Elige el
 * adaptador por nombre de proveedor, aplica el resultado a la máquina de estados del documento y deja
 * la bitácora. Cada llamada es su propia transacción.
 */
@Component
public class DocumentTransmitter {
    private final ElectronicDocumentRepository repository;
    private final ProviderConfigQueryPort configQueryPort;
    private final TransmissionLogPort transmissionLog;
    private final Map<String, ElectronicInvoiceProviderPort> providers;

    public DocumentTransmitter(ElectronicDocumentRepository repository,
                               ProviderConfigQueryPort configQueryPort,
                               TransmissionLogPort transmissionLog,
                               List<ElectronicInvoiceProviderPort> providerAdapters) {
        this.repository = repository;
        this.configQueryPort = configQueryPort;
        this.transmissionLog = transmissionLog;
        this.providers = providerAdapters.stream()
                .collect(Collectors.toMap(ElectronicInvoiceProviderPort::providerName, Function.identity()));
    }

    @Transactional
    public ElectronicDocument transmit(ElectronicDocument document) {
        ProviderConfigSnapshot config = configQueryPort.findByCompanyId(document.getCompanyId())
                .orElseThrow(() -> new IllegalStateException(
                        "La empresa no tiene un proveedor DIAN configurado."));
        ElectronicInvoiceProviderPort provider = providers.get(config.provider());
        if (provider == null) {
            throw new IllegalStateException("No hay adaptador para el proveedor: " + config.provider());
        }

        ProviderResult result = provider.transmit(document, config);
        applyResult(document, result);
        ElectronicDocument saved = repository.updateDianResult(document);

        transmissionLog.record(document.getId(), config.provider(), result.httpStatus(),
                result.providerDocumentKey(), toTransmissionResult(result.status()), result.rejectionReason());
        return saved;
    }

    private void applyResult(ElectronicDocument document, ProviderResult r) {
        switch (r.status()) {
            case VALIDADO -> document.markValidated(r.prefix(), r.consecutive(), r.cufe(), r.cude(), r.uuid(),
                    r.xmlSigned(), r.qrData(), r.qrUrl(), r.pdfRepresentation(),
                    r.validationDate() != null ? r.validationDate() : LocalDateTime.now());
            case RECHAZADO -> document.markRejected();
            case CONTINGENCIA -> document.markContingency();
            case PENDIENTE -> { /* async: el webhook completará el estado */ }
        }
    }

    private TransmissionResult toTransmissionResult(DianStatus status) {
        return switch (status) {
            case VALIDADO -> TransmissionResult.ACCEPTED;
            case RECHAZADO -> TransmissionResult.REJECTED;
            case CONTINGENCIA -> TransmissionResult.ERROR;
            case PENDIENTE -> TransmissionResult.PENDING;
        };
    }
}
