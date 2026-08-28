package com.vetsoftware.app.subscriptionbilling.infrastructure.scheduling;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobCatalog;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionBillingBatchResult;
import com.vetsoftware.app.subscriptionbilling.application.port.in.RunSubscriptionBillingCycleUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * <b>El barrido que hace que un contrato facture solo.</b> Devenga y emite el
 * periodo que toca, de todas las clinicas.
 *
 * <p>
 * <b>Cron con zona explicita, no espera entre ejecuciones.</b> Con
 * {@code fixedDelay} la hora del cierre seria la del ultimo despliegue —un
 * despliegue a las 11:00 deja el cierre corriendo a las 11:03 sobre el mismo
 * servidor que atienden las clinicas— y, peor, una hora que se mueve sola no
 * permite fijar el umbral de "este barrido no corrio", que es la unica forma
 * canonica de detectar un proceso que dejo de programarse. La cadencia y su
 * nombre viven en {@link ScheduledJobCatalog}, que es lo que ata la etiqueta
 * {@code job.name} a la alerta.
 *
 * <p>
 * <b>04:40 de Bogota</b>, la ultima de la cadena: despues del lifecycle
 * (03:10), de la cobranza (03:40) y de la reconciliacion de consumo (04:10).
 * Facturar antes de que el lifecycle cierre las lineas vencidas cobraria
 * servicios que dejaron de prestarse esa misma noche.
 *
 * <p>
 * <b>Depende del puerto y entra bajo {@link SystemAuthRunner}.</b> Un proceso
 * programado no tiene principal: sin el runner no habria autenticacion que
 * satisficiera el {@code hasRole('SYSTEM')} del puerto y el cierre moriria cada
 * madrugada con un {@code AccessDeniedException}. Pasar por el puerto es lo que
 * hace que ese gate se evalue de verdad.
 */
@Component
public class SubscriptionBillingJob {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionBillingJob.class);
    private static final ScheduledJobCatalog JOB = ScheduledJobCatalog.SUBSCRIPTION_BILLING;

    private final RunSubscriptionBillingCycleUseCase worker;
    private final SystemAuthRunner systemAuthRunner;
    private final ScheduledJobTelemetry telemetry;
    private final int batchSize;

    public SubscriptionBillingJob(RunSubscriptionBillingCycleUseCase worker,
            SystemAuthRunner systemAuthRunner, ScheduledJobTelemetry telemetry,
            @Value("${subscription.billing.batch-size:100}") int batchSize) {
        if (batchSize <= 0)
            throw new IllegalArgumentException("batchSize must be positive");
        this.worker = worker;
        this.systemAuthRunner = systemAuthRunner;
        this.telemetry = telemetry;
        this.batchSize = batchSize;
    }

    @Scheduled(cron = "${subscription.billing.cron:0 40 4 * * *}", zone = ScheduledJobCatalog.ZONE)
    public void runBilling() {
        telemetry.observe(JOB, this::executeBilling);
    }

    /**
     * <b>El desenlace distingue "no habia nada" de "fallo algo", y esa distincion
     * es la mitad del valor de la alerta.</b> Un cierre en el que todos los
     * contratos fallan es {@code FAILURE}; uno en el que fallan algunos es
     * {@code PARTIAL_FAILURE} y <b>sella el heartbeat</b>, porque quien vigila que
     * el barrido corriera no es quien vigila que no fallara.
     */
    private Outcome executeBilling() {
        long cursor = 0L;
        int processed = 0;
        int documents = 0;
        int charges = 0;
        int failures = 0;
        SubscriptionBillingBatchResult batch;
        do {
            long afterId = cursor;
            batch = systemAuthRunner.call(() -> worker.processBatchAfter(afterId, batchSize));
            processed += batch.processed();
            documents += batch.documentsIssued();
            charges += batch.chargesAccrued();
            failures += batch.failures();
            // El cursor solo puede avanzar: si el lote no lo movio, no hay mas paginas y
            // repetir la consulta seria un bucle infinito sobre el mismo id.
            if (batch.lastId() <= cursor)
                break;
            cursor = batch.lastId();
        } while (batch.processed() == batchSize);

        if (processed == 0)
            return Outcome.NO_WORK;
        log.info(
                "Facturacion recurrente finalizada: {} contrato(s), {} documento(s),"
                        + " {} cargo(s) devengado(s), {} fallo(s)",
                processed, documents, charges, failures);
        return Outcome.from(processed, failures);
    }
}
