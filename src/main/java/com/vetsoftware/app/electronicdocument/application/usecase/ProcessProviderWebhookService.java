package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.command.ProcessProviderWebhookCommand;
import com.vetsoftware.app.electronicdocument.application.port.in.ProcessProviderWebhookUseCase;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingEntitlementQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.ParsedWebhook;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigSnapshot;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderWebhookParser;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.TransmissionResult;
import com.vetsoftware.app.electronicdocument.domain.WebhookOutcome;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Procesa un webhook async (p. ej. MATIAS): enruta por proveedor, ubica el documento por la clave del
 * proveedor (bitácora), verifica HMAC con el secret de la empresa y aplica la transición DIAN.
 * Idempotente: ignora webhooks de documentos ya terminales o de claves desconocidas.
 */
@Observed(name = "electronicDocument.webhook")
@Service
public class ProcessProviderWebhookService implements ProcessProviderWebhookUseCase {
    private final ElectronicDocumentRepository repository;
    private final ProviderConfigQueryPort configQueryPort;
    private final TransmissionLogPort transmissionLog;
    private final BillingEntitlementQueryPort billingEntitlement;
    private final DocumentTransmitter documentTransmitter;
    private final Map<String, ProviderWebhookParser> parsers;

    public ProcessProviderWebhookService(ElectronicDocumentRepository repository,
                                         ProviderConfigQueryPort configQueryPort,
                                         TransmissionLogPort transmissionLog,
                                         BillingEntitlementQueryPort billingEntitlement,
                                         DocumentTransmitter documentTransmitter,
                                         List<ProviderWebhookParser> webhookParsers) {
        this.repository = repository;
        this.configQueryPort = configQueryPort;
        this.transmissionLog = transmissionLog;
        this.billingEntitlement = billingEntitlement;
        this.documentTransmitter = documentTransmitter;
        this.parsers = webhookParsers.stream()
                .collect(Collectors.toMap(ProviderWebhookParser::providerName, Function.identity()));
    }

    @Override
    @Transactional
    public void execute(ProcessProviderWebhookCommand command) {
        ProviderWebhookParser parser = parsers.get(command.provider().toUpperCase());
        if (parser == null) {
            throw new IllegalArgumentException("Proveedor de webhook desconocido: " + command.provider());
        }

        ParsedWebhook parsed = parser.parse(command.rawBody());
        if (parsed.outcome() == WebhookOutcome.IGNORED
                || parsed.providerDocumentKey() == null) {
            return; // evento no relevante / sin clave
        }

        Optional<Long> documentId = transmissionLog.findDocumentIdByProviderKey(parsed.providerDocumentKey());
        if (documentId.isEmpty()) return; // clave desconocida: ignorar (idempotente)
        ElectronicDocument document = repository.findById(documentId.get()).orElse(null);
        if (document == null) return;

        // Sin BILLING la empresa no transmite, así que no debería recibir webhooks; ignóralo por seguridad.
        if (!billingEntitlement.isElectronicInvoicingEnabled(document.getCompanyId())) return;

        ProviderConfigSnapshot config = configQueryPort.findByCompanyId(document.getCompanyId())
                .orElseThrow(() -> new IllegalStateException(
                        "La empresa del documento no tiene proveedor DIAN configurado."));

        if (!parser.verifySignature(command.rawBody(), command.signatureHeader(), config.webhookSecret())) {
            throw new AccessDeniedException("Firma de webhook inválida.");
        }

        // Idempotencia: si ya está en estado terminal, no reprocesar (webhooks pueden reintentarse).
        if (document.getDianStatus() == DianStatus.VALIDADO
                || document.getDianStatus() == DianStatus.RECHAZADO) {
            return;
        }

        if (parsed.outcome() == WebhookOutcome.ACCEPTED) {
            // El webhook NO trae el sello fiscal (CUFE/CUDE, XML firmado, QR). En vez de marcar VALIDADO con
            // el sello vacío del webhook, consultamos el estado autoritativo al proveedor (fetchStatus, vía
            // reconcile), que sí devuelve el sello; recién entonces se valida, se registra la bitácora, se
            // reversa la cartera y se entrega la representación gráfica. Si el proveedor aún no tiene el sello
            // listo, el documento queda PENDIENTE y el job de reconciliación lo reintenta — nunca se marca
            // VALIDADO sin sello.
            documentTransmitter.reconcile(document);
            return;
        }
        if (parsed.outcome() != WebhookOutcome.REJECTED) {
            return; // outcome no terminal: nada que aplicar
        }
        document.markRejected();
        repository.updateDianResult(document);
        transmissionLog.record(document.getId(), config.provider(), 200, parsed.providerDocumentKey(),
                TransmissionResult.REJECTED, parsed.rejectionReason());
    }
}
