package com.vetsoftware.app.subscriptionbilling.application.usecase;

import com.vetsoftware.app.subscriptionbilling.application.command.IssueSubscriptionPeriodDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.IssuedPeriodDocumentDto;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionBillingBatchResult;
import com.vetsoftware.app.subscriptionbilling.application.port.in.IssueSubscriptionPeriodDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.RunSubscriptionBillingCycleUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.DueSubscriptionQueryPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionPeriodAdvancePort;
import com.vetsoftware.app.subscriptionbilling.domain.BillingCycleSubscription;
import com.vetsoftware.app.subscriptionbilling.domain.BillingCycleWindow;
import com.vetsoftware.app.subscriptionbilling.domain.RecurringChargeKey;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * <b>El proceso que hace que un contrato facture solo.</b>
 *
 * <p>
 * Tres pasos por contrato, en este orden y no en otro: se devenga cada linea
 * que cobra, se emite el documento que las agrupa, y se mueve el periodo. El
 * orden importa porque es lo que hace que un reinicio converja: si el barrido
 * muere entre el segundo y el tercero, la vuelta siguiente vuelve a encontrar
 * los mismos cargos (ya sellados), la emision choca con la barandilla de
 * periodo duplicado y se salta, y el avance del periodo -que es lo unico que
 * faltaba- se ejecuta. El estado final es el mismo se muera donde se muera.
 *
 * <h2>Por que NO hay una transaccion alrededor del lote</h2>
 *
 * <p>
 * Envolver las quinientas clinicas en una sola transaccion convierte el fallo
 * de una en el rollback de las otras cuatrocientas noventa y nueve, y ademas
 * retiene una conexion del pool durante todo el cierre. Cada contrato tiene su
 * propio desenlace: lo que sostiene la correccion frente al reinicio no es el
 * {@code rollback} sino la llave de {@link RecurringChargeKey} y la barandilla
 * de {@code uq_sbd_recurring_cycle}, que son idempotentes por construccion.
 *
 * <h2>Se factura por linea en modo de pago, jamas por estado del contrato</h2>
 *
 * <p>
 * En esta clase no hay ni una lectura del estado del contrato (R-TRIAL-13). Un
 * contrato en prueba con una linea de pago obligatorio -la facturacion
 * electronica DIAN se cobra desde el dia 0- tiene que facturar esa linea.
 * Filtrar por estado deja de cobrar los modulos de pago obligatorio de todos
 * los clientes en prueba, y la factura sale bien formada, solo que mas corta:
 * no hay ninguna senal que lo delate.
 */
@Observed(name = "subscription.billing.cycle.run")
@Service
public class RunSubscriptionBillingCycleService implements RunSubscriptionBillingCycleUseCase {

    private static final Logger log = LoggerFactory
            .getLogger(RunSubscriptionBillingCycleService.class);

    private final DueSubscriptionQueryPort dueSubscriptionQueryPort;
    private final IssueSubscriptionPeriodDocumentUseCase issueUseCase;
    private final SubscriptionPeriodAdvancePort periodAdvancePort;
    private final Clock clock;

    public RunSubscriptionBillingCycleService(DueSubscriptionQueryPort dueSubscriptionQueryPort,
            IssueSubscriptionPeriodDocumentUseCase issueUseCase,
            SubscriptionPeriodAdvancePort periodAdvancePort, Clock clock) {
        this.dueSubscriptionQueryPort = dueSubscriptionQueryPort;
        this.issueUseCase = issueUseCase;
        this.periodAdvancePort = periodAdvancePort;
        this.clock = clock;
    }

    @Override
    public SubscriptionBillingBatchResult processBatchAfter(long afterId, int batchSize) {
        if (batchSize <= 0)
            throw new IllegalArgumentException("batchSize must be positive");
        if (afterId < 0)
            throw new IllegalArgumentException("afterId must not be negative");

        // El dia lo pone el reloj inyectado y nunca el llamador: un barrido que
        // pudiera elegir la fecha podria refacturar un mes ya cobrado.
        LocalDate runDate = LocalDate.now(clock);
        List<BillingCycleSubscription> due = dueSubscriptionQueryPort.dueForBillingAfter(runDate,
                afterId, batchSize);
        if (due.isEmpty())
            return new SubscriptionBillingBatchResult(0, 0, 0, 0, 0, afterId);

        long cursor = afterId;
        int processed = 0;
        int documentsIssued = 0;
        int chargesAccrued = 0;
        int skipped = 0;
        int failures = 0;
        for (BillingCycleSubscription subscription : due) {
            cursor = Math.max(cursor, subscription.id());
            processed++;
            // Segunda barrera, y no es redundante con el WHERE: el filtro SQL es un
            // superconjunto -no sabe que el primer dia devengable de un contrato en
            // prueba es el siguiente al fin de la prueba- y sin esta guarda el barrido
            // le avanzaria el periodo a un contrato que todavia no ha empezado a
            // devengar, saltandose su primer mes de cobro.
            if (!subscription.dueOn(runDate)) {
                skipped++;
                continue;
            }
            try {
                Outcome outcome = billOne(subscription);
                chargesAccrued += outcome.accrued();
                if (outcome.issued())
                    documentsIssued++;
                else
                    skipped++;
            } catch (RuntimeException exception) {
                // Un contrato que revienta no puede llevarse el cierre de los demas por
                // delante: se cuenta, se registra con su id y el barrido sigue. El
                // reintento de manana lo vuelve a encontrar porque su periodo no avanzo.
                failures++;
                log.error("No se pudo facturar el contrato {} de la empresa {}: {}",
                        subscription.id(), subscription.companyId(), exception.getMessage(),
                        exception);
            }
        }
        return new SubscriptionBillingBatchResult(processed, documentsIssued, chargesAccrued,
                skipped, failures, cursor);
    }

    /** Lo que le paso a un contrato en esta vuelta. */
    private record Outcome(boolean issued, int accrued) {
    }

    /**
     * Los dos pasos de un contrato: delega en
     * {@link IssueSubscriptionPeriodDocumentUseCase} el devengo y la emision -el
     * mismo {@code accrue()} + {@code issue()} de siempre, ver esa clase-, y avanza
     * el periodo. El avance va <b>al final y siempre que no haya excepcion</b>,
     * incluida la vuelta en la que no se emitio nada: un contrato cuyo periodo no
     * avanza vuelve a salir en el barrido de manana con el mismo periodo, y asi
     * todos los dias.
     */
    private Outcome billOne(BillingCycleSubscription subscription) {
        BillingCycleWindow window = subscription.windowToBill();
        IssuedPeriodDocumentDto result = issueUseCase
                .execute(new IssueSubscriptionPeriodDocumentCommand(subscription.companyId(),
                        subscription.id(), window.period().start(), window.period().end()));
        periodAdvancePort.advanceTo(subscription.id(), subscription.companyId(),
                window.period().start(), window.period().end(), window.nextBillingDate());
        return new Outcome(result.issued(), result.accruedCharges());
    }
}
