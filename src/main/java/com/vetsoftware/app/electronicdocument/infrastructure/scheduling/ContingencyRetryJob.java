package com.vetsoftware.app.electronicdocument.infrastructure.scheduling;

import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics.Origin;
import com.vetsoftware.app.electronicdocument.application.port.out.DianJobLeasePort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.TransmissionLogPort;
import com.vetsoftware.app.electronicdocument.application.usecase.DocumentTransmitter;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobCatalog;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricNames;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reintenta los documentos que quedaron en CONTINGENCIA (proveedor/DIAN
 * indisponible al emitir). Cada documento se retransmite en su propia
 * transacción ({@link DocumentTransmitter}); un fallo no afecta a los demás. Si
 * el reintento resuelve, el documento pasa a VALIDADO/RECHAZADO; si no, sigue
 * en CONTINGENCIA y se reintenta en el siguiente ciclo.
 *
 * <p>
 * <b>Dos pasadas diarias a hora fija</b> ({@code dian.contingency.cron}, por
 * defecto {@code 0 15 2,14 * * *} en {@code America/Bogota}), declaradas en
 * {@link com.vetsoftware.app.infrastructure.observability.ScheduledJobCatalog}.
 * La de la tarde se mantiene a proposito: la DIAN se cae por horas y esperar a
 * la madrugada siguiente alargaria la contingencia un dia entero.
 *
 * <p>
 * <b>Antes era {@code fixedDelay} de 12 h desde el fin de la pasada anterior, y
 * eso significaba que la hora era la del ultimo despliegue</b> (#609). El
 * cambio compra lo unico que no se podia tener asi: un umbral para
 * {@code VetSoftwareScheduledJobOverdue}, que detecta el barrido que <b>no
 * corrio</b> — cosa que ningun contador de fallos puede ver, porque un job que
 * no se ejecuta no incrementa nada ni escribe nada.
 *
 * <p>
 * <b>Lo que se pierde y se asume:</b> el {@code initial-delay} de 60 s daba una
 * pasada rapida tras cada despliegue, que era la unica ventana de reintento
 * corto con un ciclo de 12 h. Un documento que cae en contingencia justo
 * despues de una corrida espera ahora hasta la pasada siguiente. Un reintento
 * inmediato se pide a mano por
 * {@code POST /electronic-documents/{id}/transmit}.
 *
 * <p>
 * El reintento se acota por dos límites (configurables): un <b>cap de
 * intentos</b> ({@code
 * dian.contingency.max-attempts}) y una <b>ventana de plazo</b> desde la
 * creación ({@code
 * dian.contingency.deadline-hours}). Agotado cualquiera de los dos, el
 * documento se deja en CONTINGENCIA pero deja de reintentarse automáticamente
 * (se registra un error para atención manual: reemisión vía
 * {@code POST /electronic-documents/{id}/transmit} o corrección por nota).
 *
 * <p>
 * <b>Señal de los documentos agotados.</b> Un documento agotado no cuenta como
 * intentado, así que un lote entero de agotados devuelve {@code NO_WORK}: en la
 * métrica del job es indistinguible de un día tranquilo. Quien vigila esa
 * población es el gauge {@code vetsoftware.business.dian.contingency.exhausted}
 * (sin etiquetas, una sola serie), que {@code BusinessGaugeMetrics} calcula por
 * SQL con estos mismos dos límites. El {@code ERROR} por documento se emite
 * <b>una sola vez, en la transición</b> a agotado.
 *
 * <p>
 * <b>Esa unicidad NO es idempotente entre reinicios ni entre réplicas</b>: se
 * sostiene sobre un conjunto en memoria de este bean, que se pierde en cada
 * despliegue o reinicio y no se comparte entre réplicas. Tras un reinicio, la
 * primera pasada vuelve a emitir un {@code ERROR} por cada documento agotado
 * que reclame; con N réplicas, cada una lo emite una vez. Es una mejora frente
 * a repetirlo cada 12 h, no una solución: la solución es marcar el agotamiento
 * en persistencia y dejar de arrendar esos documentos (issue #84, punto 3), y
 * mientras tanto la fuente de verdad para alertar es el gauge, no el log.
 *
 * <p>
 * El lote se reclama con {@link DianJobLeasePort}. Este job corre en todas las
 * réplicas del backend a la vez: sin reparto, N réplicas retransmitirían el
 * mismo documento a la DIAN en el mismo ciclo, y con un proveedor que no
 * deduplique eso son documentos fiscales repetidos.
 */
@Component
public class ContingencyRetryJob {
    private static final Logger log = LoggerFactory.getLogger(ContingencyRetryJob.class);
    private static final ScheduledJobCatalog JOB = ScheduledJobCatalog.DIAN_CONTINGENCY_RETRY;
    /** Techo del recuerdo en memoria de documentos ya reportados como agotados. */
    private static final int REPORTED_EXHAUSTED_CAPACITY = 5_000;

    private final ElectronicDocumentRepository repository;
    private final DianJobLeasePort leasePort;
    private final DocumentTransmitter transmitter;
    private final TransmissionLogPort transmissionLog;
    private final ScheduledJobTelemetry telemetry;
    private final int maxAttempts;
    private final long deadlineHours;
    private final int batchSize;
    private final Duration lease;
    /**
     * Documentos por los que ya se emitió el {@code ERROR} de agotamiento.
     * <b>Estado en memoria y por réplica</b>: ver la nota de la clase.
     */
    private final Set<Long> reportedExhausted = new LinkedHashSet<>();

    public ContingencyRetryJob(ElectronicDocumentRepository repository, DianJobLeasePort leasePort,
            DocumentTransmitter transmitter, TransmissionLogPort transmissionLog,
            ScheduledJobTelemetry telemetry,
            @Value("${dian.contingency.max-attempts:4}") int maxAttempts,
            @Value("${dian.contingency.deadline-hours:48}") long deadlineHours,
            @Value("${dian.contingency.batch-size:25}") int batchSize,
            @Value("${dian.contingency.lease:PT30M}") Duration lease) {
        this.repository = repository;
        this.leasePort = leasePort;
        this.transmitter = transmitter;
        this.transmissionLog = transmissionLog;
        this.telemetry = telemetry;
        this.maxAttempts = maxAttempts;
        this.deadlineHours = deadlineHours;
        this.batchSize = batchSize;
        this.lease = lease;
    }

    @Scheduled(cron = "${dian.contingency.cron:0 15 2,14 * * *}", zone = ScheduledJobCatalog.ZONE)
    public void retryContingencies() {
        telemetry.observe(JOB, this::executeRetries);
    }

    private Outcome executeRetries() {
        List<Long> leased = leasePort.leaseByDianStatus(DianStatus.CONTINGENCIA, batchSize, lease);
        if (leased.isEmpty())
            return Outcome.NO_WORK;
        LocalDateTime deadlineThreshold = LocalDateTime.now().minusHours(deadlineHours);
        log.info("Reintentando documento(s) en contingencia DIAN: {} reclamado(s)", leased.size());
        int attempted = 0;
        int failures = 0;
        int exhausted = 0;
        for (Long documentId : leased) {
            ElectronicDocument document = repository.findById(documentId).orElse(null);
            if (document == null)
                continue;
            if (isExhausted(document, deadlineThreshold)) {
                exhausted++;
                continue;
            }
            // Volvió a ser reintentable (reemisión manual, cambio de límites): se
            // olvida para que un agotamiento posterior vuelva a registrarse.
            forgetExhausted(documentId);
            attempted++;
            try {
                transmitter.transmit(document, Origin.RETRY);
            } catch (IllegalStateException e) {
                failures++;
                // Población DETERMINISTA, separada a proposito de la transitoria: es la
                // familia de fallos que la pasada siguiente volveria a producir identica.
                // La lanza DocumentTransmitter cuando la empresa no tiene proveedor DIAN
                // configurado o no hay adaptador para el proveedor configurado, y el
                // adaptador MATIAS cuando el login no devuelve token. Ninguna se arregla
                // reintentando: fallan el 100% de los documentos de la empresa hasta que
                // alguien cambia configuracion, asi que es ERROR y no WARN. Mezclarla con
                // el WARN de abajo esconde un fallo total detras del ruido del aislado.
                // Ademas cada pasada consume uno de los 4 intentos del cap: cuatro ciclos
                // despues el documento queda agotado por un motivo que nunca fue el suyo.
                log.error(
                        "Reintento de contingencia DIAN imposible para el documento {}:"
                                + " configuración o estado inválidos. El reintento automático"
                                + " de la próxima pasada NO lo resuelve; requiere intervención.",
                        documentId, e);
            } catch (Exception e) {
                failures++;
                // Población TRANSITORIA: la vuelve a intentar la pasada siguiente, hasta
                // el cap de intentos.
                // La excepción va como último argumento, no e.getMessage(): una NPE
                // trae null y el mensaje suelto tira la cadena de causas y la traza.
                log.warn("Reintento de contingencia falló para documento {}", documentId, e);
            }
        }
        Outcome outcome = Outcome.from(attempted, failures);
        log.info(
                "Reintento de contingencias DIAN finalizado: intentado(s)={}, fallido(s)={},"
                        + " agotado(s)={}, resultado={}",
                attempted, failures, exhausted, outcome.value());
        return outcome;
    }

    /**
     * Un documento ya no se reintenta si superó el cap de intentos o la ventana de
     * plazo.
     *
     * <p>
     * El {@code ERROR} se emite en la <b>transición</b> a agotado, no en cada
     * pasada: repetirlo cada ciclo sobre los mismos documentos convierte el nivel
     * {@code ERROR} en ruido de fondo y tapa los errores reales.
     */
    private boolean isExhausted(ElectronicDocument document, LocalDateTime deadlineThreshold) {
        int attempts = transmissionLog.countAttempts(document.getId());
        if (attempts >= maxAttempts) {
            if (markExhaustedReported(document.getId())) {
                log.error(
                        "Documento {} agotó el cap de {} reintentos de contingencia DIAN; "
                                + "requiere atención manual (reemitir o corregir por nota). "
                                + "Se registra una sola vez por proceso: el conteo vivo está en "
                                + "la métrica {}.",
                        document.getId(), maxAttempts,
                        BusinessMetricNames.DIAN_CONTINGENCY_EXHAUSTED);
            }
            return true;
        }
        if (document.getCreatedDate() != null
                && document.getCreatedDate().isBefore(deadlineThreshold)) {
            if (markExhaustedReported(document.getId())) {
                log.error(
                        "Documento {} superó la ventana de {}h del plazo de contingencia DIAN; "
                                + "requiere atención manual (reemitir o corregir por nota). "
                                + "Se registra una sola vez por proceso: el conteo vivo está en "
                                + "la métrica {}.",
                        document.getId(), deadlineHours,
                        BusinessMetricNames.DIAN_CONTINGENCY_EXHAUSTED);
            }
            return true;
        }
        return false;
    }

    /**
     * Marca el documento como ya reportado y responde si esta es la primera vez.
     *
     * <p>
     * El conjunto está acotado a {@link #REPORTED_EXHAUSTED_CAPACITY} entradas y
     * descarta las más antiguas por orden de inserción: un documento desalojado
     * volvería a registrar su {@code ERROR}, que es preferible a que la memoria
     * crezca sin límite.
     */
    private boolean markExhaustedReported(Long documentId) {
        synchronized (reportedExhausted) {
            if (!reportedExhausted.add(documentId)) {
                return false;
            }
            if (reportedExhausted.size() > REPORTED_EXHAUSTED_CAPACITY) {
                var oldest = reportedExhausted.iterator();
                oldest.next();
                oldest.remove();
            }
            return true;
        }
    }

    private void forgetExhausted(Long documentId) {
        synchronized (reportedExhausted) {
            reportedExhausted.remove(documentId);
        }
    }
}
