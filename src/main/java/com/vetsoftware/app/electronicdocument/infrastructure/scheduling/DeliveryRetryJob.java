package com.vetsoftware.app.electronicdocument.infrastructure.scheduling;

import com.vetsoftware.app.electronicdocument.application.port.out.DianJobLeasePort;
import com.vetsoftware.app.electronicdocument.application.port.out.ElectronicDocumentRepository;
import com.vetsoftware.app.electronicdocument.application.usecase.DeliverElectronicDocumentService;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import java.time.Clock;
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
 * Reintenta la <b>entrega</b> —QR, PDF, S3 y correo— de los documentos que la
 * DIAN ya validó pero que se quedaron sin representación gráfica (issue #204).
 *
 * <p>
 * <b>El agujero que tapa.</b> {@code deliverIfValidated} se llama en línea
 * justo después de emitir, en el {@code afterCommit} del cierre de cuenta
 * ({@code ClosedAccountEmissionCompleter}) y en el tiempo 3 de la venta POS
 * ({@code RegisterPosSaleService}). Si el render del PDF, el generador de QR o
 * S3 revientan ahí, la excepción sube, se registra y ahí muere: el documento
 * queda VALIDADO y sin PDF. La factura existe y es fiscalmente válida ante la
 * DIAN, pero el cliente nunca recibe su representación gráfica y el mostrador
 * no puede reimprimirla. Ninguna de las tres rutas que podrían rescatarlo lo
 * hacía, porque las tres filtran por PENDIENTE.
 *
 * <p>
 * <b>Reintenta la entrega, nunca la emisión.</b> Este job no habla con el
 * proveedor ni con la DIAN: el documento ya está validado y retransmitirlo
 * emitiría un segundo documento fiscal. Solo invoca
 * {@link DeliverElectronicDocumentService#deliverIfValidated}, que es
 * idempotente por su propio guard —en cuanto el PDF queda adjunto, la pasada
 * siguiente ya no lo arrienda porque la sentencia del lease filtra por
 * {@code pdf_representation IS NULL}.
 *
 * <p>
 * <b>El ciclo es de 12 h</b> ({@code dian.delivery.retry-delay-ms}), contadas
 * desde que TERMINA la pasada anterior; el reloj se reinicia en cada
 * despliegue.
 *
 * <p>
 * <b>Cota de reintentos: ventana de plazo, sin cap de intentos.</b>
 * {@code ContingencyRetryJob} acota por dos límites —cap de intentos y ventana
 * desde la creación—; aquí solo existe el segundo
 * ({@code dian.delivery.deadline-hours}, 72 h por defecto ≈ 6 pasadas). El cap
 * de intentos de aquel se apoya en {@code transmission_log.countAttempts}, que
 * cuenta transmisiones a la DIAN y no dice nada sobre cuántas veces se intentó
 * entregar; un cap honesto exigiría persistir un contador de intentos de
 * entrega, y eso es un cambio de esquema que no entra en este arreglo. La
 * ventana sí es una cota dura y está respaldada por una columna que ya existe
 * ({@code created_date}), así que ningún documento se reintenta
 * indefinidamente.
 *
 * <p>
 * <b>Lo que la ventana NO evita</b>, igual que en el job de contingencia: un
 * documento agotado se sigue arrendando en cada pasada y consume una plaza del
 * lote. Con pocos agotados es ruido; si la población creciera, taparía a los
 * recuperables. Cerrarlo exige marcar el agotamiento en persistencia y dejar de
 * arrendarlos, que es el mismo pendiente del issue #84 punto 3.
 *
 * <p>
 * El {@code ERROR} por documento agotado se emite <b>una sola vez, en la
 * transición</b>, sobre un conjunto en memoria de este bean: no es idempotente
 * entre reinicios ni entre réplicas. Es una mejora frente a repetirlo cada 12
 * h, no una solución.
 */
@Component
public class DeliveryRetryJob {
    private static final Logger log = LoggerFactory.getLogger(DeliveryRetryJob.class);
    // Bajo el prefijo «dian.» como sus dos hermanos, aunque este job no hable con
    // la DIAN: comparte su arriendo y su ciclo de vida, y el label queda agrupable
    // en un solo job.name=~"dian.*" en los tableros.
    private static final String JOB_NAME = "dian.delivery.retry";
    /** Techo del recuerdo en memoria de documentos ya reportados como agotados. */
    private static final int REPORTED_EXHAUSTED_CAPACITY = 5_000;

    private final ElectronicDocumentRepository repository;
    private final DianJobLeasePort leasePort;
    private final DeliverElectronicDocumentService deliverService;
    private final ScheduledJobTelemetry telemetry;
    /**
     * Inyectado y no {@code LocalDateTime.now()} a secas: la ventana de plazo es
     * toda la cota de reintentos que tiene este job, y un limite que no se puede
     * fijar desde un test es un limite que nadie comprueba. Ademas lo exige la
     * regla {@code RELOJ_DEL_SISTEMA} de {@code HexagonalArchitectureTest}, que
     * nace con 167 violaciones congeladas y no admite ninguna nueva.
     */
    private final Clock clock;
    private final long deadlineHours;
    private final int batchSize;
    private final Duration lease;
    /**
     * Documentos por los que ya se emitió el {@code ERROR} de agotamiento.
     * <b>Estado en memoria y por réplica</b>: ver la nota de la clase.
     */
    private final Set<Long> reportedExhausted = new LinkedHashSet<>();

    public DeliveryRetryJob(ElectronicDocumentRepository repository, DianJobLeasePort leasePort,
            DeliverElectronicDocumentService deliverService, ScheduledJobTelemetry telemetry,
            Clock clock, @Value("${dian.delivery.deadline-hours:72}") long deadlineHours,
            @Value("${dian.delivery.batch-size:25}") int batchSize,
            @Value("${dian.delivery.lease:PT15M}") Duration lease) {
        this.repository = repository;
        this.leasePort = leasePort;
        this.deliverService = deliverService;
        this.telemetry = telemetry;
        this.clock = clock;
        this.deadlineHours = deadlineHours;
        this.batchSize = batchSize;
        this.lease = lease;
    }

    @Scheduled(initialDelayString = "${dian.delivery.initial-delay-ms:180000}", fixedDelayString = "${dian.delivery.retry-delay-ms:43200000}")
    public void retryDeliveries() {
        telemetry.observe(JOB_NAME, this::executeRetries);
    }

    private Outcome executeRetries() {
        List<Long> leased = leasePort.leaseUndeliveredValidated(batchSize, lease);
        if (leased.isEmpty())
            return Outcome.NO_WORK;
        LocalDateTime deadlineThreshold = LocalDateTime.now(clock).minusHours(deadlineHours);
        log.info("Reintentando la entrega de documento(s) VALIDADO sin representación gráfica:"
                + " {} reclamado(s)", leased.size());
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
            // Volvió a ser reintentable (cambio de límites, reentrega manual que dejó
            // otra vez el documento sin PDF): se olvida para que un agotamiento
            // posterior vuelva a registrarse.
            forgetExhausted(documentId);
            attempted++;
            try {
                deliverService.deliverIfValidated(document);
            } catch (Exception e) {
                failures++;
                // Todo lo que falla aquí es transitorio por defecto —S3, el render del
                // PDF, un OptimisticLock contra otro job— y la pasada siguiente lo vuelve
                // a intentar hasta agotar la ventana. Por eso WARN: hay recuperación
                // automática. Lo que no la tiene es el agotamiento, y ese sí es ERROR.
                // La excepción va como último argumento, no e.getMessage(): una NPE trae
                // null y el mensaje suelto tira la cadena de causas y la traza.
                log.warn("Reintento de entrega falló para documento {}", documentId, e);
            }
        }
        Outcome outcome = Outcome.from(attempted, failures);
        log.info(
                "Reintento de entregas finalizado: intentado(s)={}, fallido(s)={},"
                        + " agotado(s)={}, resultado={}",
                attempted, failures, exhausted, outcome.value());
        return outcome;
    }

    /**
     * Un documento deja de reintentarse cuando supera la ventana de plazo desde su
     * creación.
     *
     * <p>
     * Un {@code createdDate} nulo NO se da por agotado: es un documento sin fecha
     * con el que no se puede calcular la ventana, y descartarlo para siempre por un
     * dato ausente sería exactamente el silencio que este job viene a eliminar.
     */
    private boolean isExhausted(ElectronicDocument document, LocalDateTime deadlineThreshold) {
        if (document.getCreatedDate() == null
                || !document.getCreatedDate().isBefore(deadlineThreshold)) {
            return false;
        }
        if (markExhaustedReported(document.getId())) {
            log.error("Documento {} lleva más de {}h VALIDADO sin representación gráfica y deja de"
                    + " reintentarse solo; el cliente no tiene su factura y el mostrador no"
                    + " puede reimprimirla. Requiere atención manual: reentregar con POST"
                    + " /electronic-documents/{}/transmit. Se registra una sola vez por"
                    + " proceso.", document.getId(), deadlineHours, document.getId());
        }
        return true;
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
