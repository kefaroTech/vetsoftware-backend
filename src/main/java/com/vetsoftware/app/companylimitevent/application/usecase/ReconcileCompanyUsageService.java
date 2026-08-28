package com.vetsoftware.app.companylimitevent.application.usecase;

import com.vetsoftware.app.companylimitevent.application.command.RecordLimitEventCommand;
import com.vetsoftware.app.companylimitevent.application.dto.UsageReconciliationDto;
import com.vetsoftware.app.companylimitevent.application.port.in.ReconcileCompanyUsageUseCase;
import com.vetsoftware.app.companylimitevent.application.port.in.RecordLimitEventUseCase;
import com.vetsoftware.app.companylimitevent.application.port.out.CapacityCounterPort;
import com.vetsoftware.app.companylimitevent.application.port.out.CapacityCounterPort.CapacityCounter;
import com.vetsoftware.app.companylimitevent.application.port.out.RealUsageCountPort;
import com.vetsoftware.app.companylimitevent.domain.EventActor;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import com.vetsoftware.app.companylimitevent.domain.LimitSource;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compara el contador con las filas reales, eje por eje, y deja escrito el
 * resultado.
 *
 * <p>
 * <strong>Sin transaccion propia sobre el lote</strong>, y es deliberado: el
 * hecho de desvio se escribe con {@code RecordLimitEventUseCase}, que va en
 * transaccion independiente, y el sello es una sentencia por contador. Envolver
 * el lote entero en una sola transaccion haria que un contador que falla al
 * final tirara el trabajo de los doscientos anteriores --y el recuento volveria
 * a empezar de cero cada noche sin avanzar nunca--.
 *
 * <p>
 * <strong>Un contador con desvio no se sella.</strong> Se queda pendiente a
 * proposito, para que la siguiente pasada lo vuelva a mirar: si el desvio venia
 * de un movimiento perdido y alguien lo corrigio con la valvula de D-12, la
 * pasada siguiente lo encuentra cuadrado y lo sella. Sellarlo aqui lo sacaria
 * del indice de pendientes justo cuando es el que mas hay que vigilar.
 */
@Service
public class ReconcileCompanyUsageService implements ReconcileCompanyUsageUseCase {

    private static final Logger log = LoggerFactory.getLogger(ReconcileCompanyUsageService.class);

    /** Lo que se escribe en el hecho para poder buscarlo despues. */
    private static final String REASON_CODE = "USAGE_DRIFT";

    private final CapacityCounterPort capacityCounterPort;
    private final RealUsageCountPort realUsageCountPort;
    private final RecordLimitEventUseCase recordLimitEvent;
    private final Clock clock;

    public ReconcileCompanyUsageService(CapacityCounterPort capacityCounterPort,
            RealUsageCountPort realUsageCountPort, RecordLimitEventUseCase recordLimitEvent,
            Clock clock) {
        this.capacityCounterPort = capacityCounterPort;
        this.realUsageCountPort = realUsageCountPort;
        this.recordLimitEvent = recordLimitEvent;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UsageReconciliationDto execute(LocalDateTime staleBefore, long afterId, int batchSize) {
        if (batchSize <= 0)
            throw new IllegalArgumentException("batch size must be positive");
        // El instante se toma UNA vez, antes de contar, y es el que sella todo el
        // lote: es el momento en que estas cifras eran verdad. Tomarlo al escribir
        // cada sello afirmaria una frescura posterior al recuento que la comprobo.
        LocalDateTime countedAt = LocalDateTime.now(clock);
        List<CapacityCounter> counters = capacityCounterPort.findUnreconciled(staleBefore, afterId,
                batchSize);

        int matched = 0;
        int drifted = 0;
        int skipped = 0;
        long lastId = afterId;
        for (CapacityCounter counter : counters) {
            // El cursor avanza SIEMPRE, examine o no el contador. Dejarlo quieto en
            // los saltados dejaria al barrido reintentando eternamente los ejes que
            // hoy no se pueden contar --que son cinco de los ocho--.
            lastId = counter.id();
            OptionalInt real = realUsageCountPort.countFor(counter.companyId(),
                    counter.dimensionCode());
            if (real.isEmpty()) {
                // Eje sin fuente de verdad computable hoy. Ni hecho ni sello: ver el
                // razonamiento entero en RealUsageCountPort.
                skipped++;
                continue;
            }
            int deviation = real.getAsInt() - counter.usedQuantity();
            if (deviation == 0) {
                capacityCounterPort.markReconciled(counter.companyId(), counter.limitDimensionId(),
                        counter.periodKey(), countedAt);
                matched++;
                continue;
            }
            recordDrift(counter, deviation);
            drifted++;
        }
        if (drifted > 0)
            log.warn("Recuento de consumo: {} contador(es) con desvio sobre {} examinado(s)",
                    drifted, counters.size());
        return new UsageReconciliationDto(counters.size(), matched, drifted, skipped, lastId);
    }

    /**
     * El hecho compensatorio, con los tres numeros del momento copiados dentro
     * (R-LIMIT-32): dentro de un año el techo habra cambiado y esta fila tiene que
     * seguir diciendo la verdad de hoy.
     *
     * <p>
     * {@code requestedDelta} lleva el <strong>desvio</strong> --lo que habria que
     * sumar al contador para que cuadre--, que es lo que convierte la fila en algo
     * accionable: quien corrija con la valvula de D-12 tiene el numero delante y no
     * tiene que volver a contar.
     *
     * <p>
     * El actor es el proceso y el origen del techo es {@code NONE}: el recuento no
     * resuelve precedencias, solo compara. Se invoca por el puerto y no por un
     * metodo de esta clase porque la propagacion independiente la aplica el proxy,
     * y una llamada interna la esquivaria sin avisar.
     */
    private void recordDrift(CapacityCounter counter, int deviation) {
        recordLimitEvent.execute(new RecordLimitEventCommand(counter.companyId(),
                counter.limitDimensionId(), LimitEventType.USAGE_RECONCILED,
                counter.limitQuantity(), counter.usedQuantity(), deviation, LimitSource.NONE, null,
                EventActor.automatedProcess(), REASON_CODE,
                "Counter says " + counter.usedQuantity() + " for dimension "
                        + counter.dimensionCode() + " period " + counter.periodKey()
                        + ", real rows are " + (counter.usedQuantity() + deviation)));
    }
}
