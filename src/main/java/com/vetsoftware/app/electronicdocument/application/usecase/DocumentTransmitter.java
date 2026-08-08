package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.BillingEntitlementQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics.Origin;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicInvoiceProviderPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigSnapshot;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderResult;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Núcleo de transmisión a la DIAN, reutilizable y SIN control de acceso (lo
 * invocan tanto el caso de uso con `@PreAuthorize` como el job de contingencia,
 * que corre sin contexto de seguridad). Elige el adaptador por nombre de
 * proveedor, aplica el resultado a la máquina de estados del documento y deja
 * la bitacora. El HTTP corre SIN transaccion; el desenlace se guarda aparte.
 */
@Component
public class DocumentTransmitter {
    private static final Logger log = LoggerFactory.getLogger(DocumentTransmitter.class);

    private final ProviderConfigQueryPort configQueryPort;
    private final TransmissionLogPort transmissionLog;
    private final DeliverElectronicDocumentService deliverService;
    private final BillingEntitlementQueryPort billingEntitlement;
    private final TransmissionResultPersister resultPersister;
    private final BillingMetrics billingMetrics;
    private final Map<String, ElectronicInvoiceProviderPort> providers;

    public DocumentTransmitter(ProviderConfigQueryPort configQueryPort,
            TransmissionLogPort transmissionLog, DeliverElectronicDocumentService deliverService,
            BillingEntitlementQueryPort billingEntitlement,
            TransmissionResultPersister resultPersister, BillingMetrics billingMetrics,
            List<ElectronicInvoiceProviderPort> providerAdapters) {
        this.configQueryPort = configQueryPort;
        this.transmissionLog = transmissionLog;
        this.deliverService = deliverService;
        this.billingEntitlement = billingEntitlement;
        this.resultPersister = resultPersister;
        this.billingMetrics = billingMetrics;
        this.providers = providerAdapters.stream().collect(
                Collectors.toMap(ElectronicInvoiceProviderPort::providerName, Function.identity()));
    }

    /**
     * Sin {@code @Transactional} a proposito: la llamada al proveedor tarda hasta
     * 75 segundos (15 de connect + 60 de read) y no puede correr dentro de ninguna
     * transaccion. Con la propagacion por defecto REQUIRED esto se unia a la
     * transaccion del caller y retenia su conexion del pool —y sus locks— durante
     * todo el HTTP: diez ventas concurrentes con el proveedor lento agotaban las
     * diez conexiones de Hikari y dejaban sin responder al sistema entero, no solo
     * a facturacion.
     *
     * <p>
     * Ahora las lecturas de configuracion van por su cuenta, el HTTP corre sin
     * transaccion, y el desenlace se guarda en una transaccion propia y corta a
     * traves de {@link TransmissionResultPersister}. Los callers tampoco pueden
     * tener una transaccion abierta: por eso los casos de uso de emision commitean
     * el documento PENDIENTE antes de llamar aca.
     */
    public ElectronicDocument transmit(ElectronicDocument document) {
        return transmitInternal(document, Origin.INITIAL);
    }

    public ElectronicDocument transmit(ElectronicDocument document, Origin origin) {
        return transmitInternal(document, origin);
    }

    private ElectronicDocument transmitInternal(ElectronicDocument document, Origin origin) {
        long startedAt = System.nanoTime();
        try {
            // Gate de facturación electrónica: sin submódulo BILLING nunca se contacta al
            // proveedor
            // (MATIAS).
            // El documento se deja como está (PENDIENTE si nunca se transmitió): datos
            // guardados, emisión
            // diferida
            // y re-emitible al habilitar el módulo. No se degrada a NO_ELECTRONICO.
            if (!billingEntitlement.isElectronicInvoicingEnabled(document.getCompanyId())) {
                return document;
            }

            ProviderConfigSnapshot config = configQueryPort.findByCompanyId(document.getCompanyId())
                    .orElseThrow(() -> new IllegalStateException(
                            "La empresa no tiene un proveedor DIAN configurado."));
            ElectronicInvoiceProviderPort provider = providers.get(config.provider());
            if (provider == null) {
                throw new IllegalStateException(
                        "No hay adaptador para el proveedor: " + config.provider());
            }

            // El HTTP, fuera de toda transaccion.
            ProviderResult result = provider.transmit(document, config);
            // El desenlace se guarda en una transaccion propia y corta que se abre recien
            // ahora. Reintentar un CONTINGENCIA que ahora valida registra "sano" y
            // desactiva el modo solo; no cambia el resultado de la emision.
            ElectronicDocument saved = resultPersister.persist(document, result, config.provider());
            billingMetrics.finished(result.status(), origin, saved.getDocumentType(),
                    elapsedSince(startedAt));
            return saved;
        } catch (RuntimeException | Error exception) {
            billingMetrics.failed(origin, document.getDocumentType(), elapsedSince(startedAt));
            throw exception;
        }
    }

    /**
     * Reconcilia un documento PENDIENTE consultando al proveedor su estado actual
     * (respaldo ante webhooks perdidos en proveedores asíncronos como MATIAS). Si
     * el proveedor no soporta polling (síncronos) o el documento sigue en cola, no
     * hace nada. Si el proveedor reporta un terminal, aplica la transición, deja
     * bitácora, ejecuta el reverso de cartera y entrega la representación — igual
     * que el webhook.
     *
     * <p>
     * Sin @Transactional por el mismo motivo que {@link #transmit}: aca tambien hay
     * un HTTP al proveedor que no puede correr dentro de una transaccion.
     */
    public ElectronicDocument reconcile(ElectronicDocument document) {
        return reconcileInternal(document, Origin.RECONCILIATION);
    }

    public ElectronicDocument reconcile(ElectronicDocument document, Origin origin) {
        return reconcileInternal(document, origin);
    }

    private ElectronicDocument reconcileInternal(ElectronicDocument document, Origin origin) {
        long startedAt = System.nanoTime();
        try {
            if (document.getDianStatus() != DianStatus.PENDIENTE)
                return document;
            // Sin BILLING: nunca se consulta al proveedor (los NO_ELECTRONICO ya quedan
            // fuera por el
            // filtro de estado).
            if (!billingEntitlement.isElectronicInvoicingEnabled(document.getCompanyId()))
                return document;

            ProviderConfigSnapshot config = configQueryPort.findByCompanyId(document.getCompanyId())
                    .orElseThrow(() -> new IllegalStateException(
                            "La empresa no tiene un proveedor DIAN configurado."));
            ElectronicInvoiceProviderPort provider = providers.get(config.provider());
            if (provider == null) {
                throw new IllegalStateException(
                        "No hay adaptador para el proveedor: " + config.provider());
            }

            String providerKey = transmissionLog.findLatestProviderKey(document.getId())
                    .orElse(null);
            if (providerKey == null)
                return document; // nunca se transmitió: nada que reconciliar

            Optional<ProviderResult> maybeResult = provider.fetchStatus(providerKey, config);
            if (maybeResult.isEmpty())
                return document; // proveedor síncrono / sin polling
            ProviderResult result = maybeResult.get();
            if (result.status() == DianStatus.PENDIENTE) {
                billingMetrics.finished(result.status(), origin, document.getDocumentType(),
                        elapsedSince(startedAt));
                return document; // sigue en cola
            }

            ElectronicDocument saved = resultPersister.persistReconciled(document, result,
                    config.provider(), providerKey);
            deliverService.deliverIfValidated(saved);
            billingMetrics.finished(result.status(), origin, saved.getDocumentType(),
                    elapsedSince(startedAt));
            return saved;
        } catch (RuntimeException | Error exception) {
            billingMetrics.failed(origin, document.getDocumentType(), elapsedSince(startedAt));
            throw exception;
        }
    }

    private static Duration elapsedSince(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt);
    }
}
