package com.vetsoftware.app.electronicdocument.application.usecase;

import com.vetsoftware.app.electronicdocument.application.port.out.BillingEntitlementQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics.Origin;
import com.vetsoftware.app.electronicdocument.application.port.out.ContingencyMonitorPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicInvoiceProviderPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigQueryPort;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderConfigSnapshot;
import com.vetsoftware.app.electronicdocument.application.port.out.ProviderResult;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.electronicdocument.domain.TransmissionResult;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Núcleo de transmisión a la DIAN, reutilizable y SIN control de acceso (lo invocan tanto el caso
 * de uso con `@PreAuthorize` como el job de contingencia, que corre sin contexto de seguridad).
 * Elige el adaptador por nombre de proveedor, aplica el resultado a la máquina de estados del
 * documento y deja la bitácora. Cada llamada es su propia transacción.
 */
@Component
public class DocumentTransmitter {
  private static final Logger log = LoggerFactory.getLogger(DocumentTransmitter.class);

  private final ElectronicDocumentRepository repository;
  private final ProviderConfigQueryPort configQueryPort;
  private final TransmissionLogPort transmissionLog;
  private final CreditNoteReversalApplier reversalApplier;
  private final DeliverElectronicDocumentService deliverService;
  private final BillingEntitlementQueryPort billingEntitlement;
  private final NumberAssigner numberAssigner;
  private final ContingencyMonitorPort contingencyMonitor;
  private final BillingMetrics billingMetrics;
  private final Map<String, ElectronicInvoiceProviderPort> providers;

  public DocumentTransmitter(
      ElectronicDocumentRepository repository,
      ProviderConfigQueryPort configQueryPort,
      TransmissionLogPort transmissionLog,
      CreditNoteReversalApplier reversalApplier,
      DeliverElectronicDocumentService deliverService,
      BillingEntitlementQueryPort billingEntitlement,
      NumberAssigner numberAssigner,
      ContingencyMonitorPort contingencyMonitor,
      BillingMetrics billingMetrics,
      List<ElectronicInvoiceProviderPort> providerAdapters) {
    this.repository = repository;
    this.configQueryPort = configQueryPort;
    this.transmissionLog = transmissionLog;
    this.reversalApplier = reversalApplier;
    this.deliverService = deliverService;
    this.billingEntitlement = billingEntitlement;
    this.numberAssigner = numberAssigner;
    this.contingencyMonitor = contingencyMonitor;
    this.billingMetrics = billingMetrics;
    this.providers =
        providerAdapters.stream()
            .collect(
                Collectors.toMap(ElectronicInvoiceProviderPort::providerName, Function.identity()));
  }

  @Transactional
  public ElectronicDocument transmit(ElectronicDocument document) {
    return transmitInternal(document, Origin.INITIAL);
  }

  @Transactional
  public ElectronicDocument transmit(ElectronicDocument document, Origin origin) {
    return transmitInternal(document, origin);
  }

  private ElectronicDocument transmitInternal(ElectronicDocument document, Origin origin) {
    long startedAt = System.nanoTime();
    try {
      // Gate de facturación electrónica: sin submódulo BILLING nunca se contacta al proveedor
      // (MATIAS).
      // El documento se deja como está (PENDIENTE si nunca se transmitió): datos guardados, emisión
      // diferida
      // y re-emitible al habilitar el módulo. No se degrada a NO_ELECTRONICO.
      if (!billingEntitlement.isElectronicInvoicingEnabled(document.getCompanyId())) {
        return document;
      }

      ProviderConfigSnapshot config =
          configQueryPort
              .findByCompanyId(document.getCompanyId())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "La empresa no tiene un proveedor DIAN configurado."));
      ElectronicInvoiceProviderPort provider = providers.get(config.provider());
      if (provider == null) {
        throw new IllegalStateException("No hay adaptador para el proveedor: " + config.provider());
      }

      ProviderResult result = provider.transmit(document, config);
      // Detección automática del modo contingencia: CONTINGENCIA = fallo de infraestructura
      // (5xx/timeout);
      // cualquier otro estado = el proveedor respondió (sano). Reintentar un CONTINGENCIA que ahora
      // valida
      // registra "sano" y desactiva el modo solo. No cambia el resultado de la emisión.
      contingencyMonitor.recordOutcome(
          document.getCompanyId(), result.status() != DianStatus.CONTINGENCIA);
      applyResult(document, result);
      ElectronicDocument saved = repository.updateDianResult(document);

      transmissionLog.record(
          document.getId(),
          config.provider(),
          result.httpStatus(),
          result.providerDocumentKey(),
          toTransmissionResult(result.status()),
          result.rejectionReason());

      // Subordinacion del void: si esta transmision dejo VALIDADA una nota credito (proveedor
      // sincrono),
      // reversa la factura referenciada y la cartera en el acto. Para async no pasa nada aqui
      // (PENDIENTE).
      reversalApplier.applyIfCreditNoteValidated(saved);
      billingMetrics.finished(
          result.status(), origin, saved.getDocumentType(), elapsedSince(startedAt));
      return saved;
    } catch (RuntimeException | Error exception) {
      billingMetrics.failed(origin, document.getDocumentType(), elapsedSince(startedAt));
      throw exception;
    }
  }

  /**
   * Reconcilia un documento PENDIENTE consultando al proveedor su estado actual (respaldo ante
   * webhooks perdidos en proveedores asíncronos como MATIAS). Si el proveedor no soporta polling
   * (síncronos) o el documento sigue en cola, no hace nada. Si el proveedor reporta un terminal,
   * aplica la transición, deja bitácora, ejecuta el reverso de cartera y entrega la representación
   * — igual que el webhook.
   */
  @Transactional
  public ElectronicDocument reconcile(ElectronicDocument document) {
    return reconcileInternal(document, Origin.RECONCILIATION);
  }

  @Transactional
  public ElectronicDocument reconcile(ElectronicDocument document, Origin origin) {
    return reconcileInternal(document, origin);
  }

  private ElectronicDocument reconcileInternal(ElectronicDocument document, Origin origin) {
    long startedAt = System.nanoTime();
    try {
      if (document.getDianStatus() != DianStatus.PENDIENTE) return document;
      // Sin BILLING: nunca se consulta al proveedor (los NO_ELECTRONICO ya quedan fuera por el
      // filtro de estado).
      if (!billingEntitlement.isElectronicInvoicingEnabled(document.getCompanyId()))
        return document;

      ProviderConfigSnapshot config =
          configQueryPort
              .findByCompanyId(document.getCompanyId())
              .orElseThrow(
                  () ->
                      new IllegalStateException(
                          "La empresa no tiene un proveedor DIAN configurado."));
      ElectronicInvoiceProviderPort provider = providers.get(config.provider());
      if (provider == null) {
        throw new IllegalStateException("No hay adaptador para el proveedor: " + config.provider());
      }

      String providerKey = transmissionLog.findLatestProviderKey(document.getId()).orElse(null);
      if (providerKey == null) return document; // nunca se transmitió: nada que reconciliar

      Optional<ProviderResult> maybeResult = provider.fetchStatus(providerKey, config);
      if (maybeResult.isEmpty()) return document; // proveedor síncrono / sin polling
      ProviderResult result = maybeResult.get();
      if (result.status() == DianStatus.PENDIENTE) {
        billingMetrics.finished(
            result.status(), origin, document.getDocumentType(), elapsedSince(startedAt));
        return document; // sigue en cola
      }

      applyResult(document, result);
      ElectronicDocument saved = repository.updateDianResult(document);
      transmissionLog.record(
          document.getId(),
          config.provider(),
          result.httpStatus(),
          providerKey,
          toTransmissionResult(result.status()),
          result.rejectionReason());

      reversalApplier.applyIfCreditNoteValidated(saved);
      deliverService.deliverIfValidated(saved);
      billingMetrics.finished(
          result.status(), origin, saved.getDocumentType(), elapsedSince(startedAt));
      return saved;
    } catch (RuntimeException | Error exception) {
      billingMetrics.failed(origin, document.getDocumentType(), elapsedSince(startedAt));
      throw exception;
    }
  }

  private void applyResult(ElectronicDocument document, ProviderResult r) {
    switch (r.status()) {
      case VALIDADO -> {
        // Alerta de seguridad: un documento NUNCA debería quedar VALIDADO sin sello fiscal
        // (CUFE en factura / CUDE en POS y notas). El proveedor ya degrada a PENDIENTE el "00 sin
        // sello"; esto es la red por si otra ruta/proveedor lo marcara validado sin CUFE/CUDE.
        if (isBlank(r.cufe()) && isBlank(r.cude())) {
          log.error(
              "Documento {} marcado VALIDADO SIN SELLO (CUFE/CUDE vacíos). "
                  + "Revisar la respuesta del proveedor; requiere atención manual.",
              document.getId());
        }
        document.markValidated(
            r.prefix(),
            r.consecutive(),
            r.cufe(),
            r.cude(),
            r.uuid(),
            r.xmlSigned(),
            r.qrData(),
            r.qrUrl(),
            r.pdfRepresentation(),
            r.validationDate() != null ? r.validationDate() : LocalDateTime.now());
      }
      case RECHAZADO -> {
        document.markRejected();
        // Recupera el consecutivo (si es seguro) para no dejar un hueco en la secuencia fiscal.
        // El persist posterior (updateDianResult) guarda la numeración limpia.
        numberAssigner.release(document);
      }
      case CONTINGENCIA -> document.markContingency();
      case PENDIENTE -> {
        /* async: el webhook completará el estado */
      }
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }

  private TransmissionResult toTransmissionResult(DianStatus status) {
    return switch (status) {
      case VALIDADO -> TransmissionResult.ACCEPTED;
      case RECHAZADO -> TransmissionResult.REJECTED;
      case CONTINGENCIA -> TransmissionResult.ERROR;
      case PENDIENTE -> TransmissionResult.PENDING;
      case NO_ELECTRONICO ->
          throw new IllegalStateException("NO_ELECTRONICO no es un resultado de transmisión");
    };
  }

  private static Duration elapsedSince(long startedAt) {
    return Duration.ofNanos(System.nanoTime() - startedAt);
  }
}
