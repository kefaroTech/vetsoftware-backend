package com.vetsoftware.app.quote.infrastructure.scheduling;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobCatalog;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry;
import com.vetsoftware.app.infrastructure.observability.ScheduledJobTelemetry.Outcome;
import com.vetsoftware.app.quote.application.port.in.ExpireOverdueQuotesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Barrido diario que caduca las cotizaciones vencidas.
 *
 * <p>
 * <strong>Existe porque {@code valid_until} no se cumplia solo.</strong>
 * {@code POST /quotes/expire-overdue} estaba en el contrato y no lo invocaba
 * nadie: ni un {@code @Scheduled} ni una regla de EventBridge. Sin el, una
 * cotizacion se queda en {@code SENT} para siempre por mucho que su vigencia
 * haya pasado, la consola sigue ofreciendo «Marcar aceptada», y un operador
 * puede aceptar en agosto una oferta cuyo precio se congelo en marzo — que se
 * vuelve contrato y devenga esos importes. Incidencia #443.
 *
 * <p>
 * <strong>Y no hay boton.</strong> La consola decidio deliberadamente no
 * exponerlo: un barrido masivo disparado a mano desde una interfaz es como se
 * ejecuta dos veces. El endpoint se queda para operar a mano cuando haga falta;
 * quien lo cumple a diario es esto.
 *
 * <p>
 * <strong>Sin cerrojo distribuido, y es seguro.</strong> Cada tarea de Fargate
 * ejecuta su copia, como el resto de los {@code @Scheduled} del arbol
 * ({@code DianJobLeasePort} lo deja escrito). Aqui no hace falta arbitrar: el
 * caso de uso carga y guarda entidad a entidad, {@code quotes} va versionada,
 * asi que dos copias que topen con la misma fila producen un
 * {@code ObjectOptimisticLockingFailureException} en la segunda en vez de una
 * escritura perdida — y marcar EXPIRED dos veces lo rechaza igual la propia
 * transicion de estado del dominio. Lo peor que pasa es que un lote se repita.
 *
 * <p>
 * El {@code initialDelay} escalona el arranque frente a los otros barridos del
 * modelo de suscripciones para no arrancar cinco trabajos contra el mismo pool
 * de conexiones en el mismo segundo.
 */
@Component
public class QuoteExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(QuoteExpirationJob.class);
    private static final ScheduledJobCatalog JOB = ScheduledJobCatalog.QUOTE_EXPIRATION;

    private final ExpireOverdueQuotesUseCase expireOverdueUseCase;
    private final SystemAuthRunner systemAuthRunner;
    private final ScheduledJobTelemetry telemetry;
    private final int batchSize;

    public QuoteExpirationJob(ExpireOverdueQuotesUseCase expireOverdueUseCase,
            SystemAuthRunner systemAuthRunner, ScheduledJobTelemetry telemetry,
            @Value("${quote.expiration.batch-size:200}") int batchSize) {
        if (batchSize <= 0)
            throw new IllegalArgumentException("batchSize must be positive");
        this.expireOverdueUseCase = expireOverdueUseCase;
        this.systemAuthRunner = systemAuthRunner;
        this.telemetry = telemetry;
        this.batchSize = batchSize;
    }

    @Scheduled(cron = "${quote.expiration.cron:0 25 3 * * *}", zone = ScheduledJobCatalog.ZONE)
    public void runExpiration() {
        telemetry.observe(JOB, this::expireOverdue);
    }

    /**
     * Repite mientras el lote salga lleno: el tope existe para acotar la
     * transaccion, no para acotar el trabajo, y pararse en el primer lote dejaria
     * cotizaciones vencidas vivas hasta el dia siguiente.
     *
     * <p>
     * El {@code systemAuthRunner} es obligatorio: el puerto va cerrado a
     * {@code hasRole('SYSTEM')} y un hilo del planificador no trae principal, asi
     * que sin el la llamada muere con {@code AccessDeniedException}.
     */
    private Outcome expireOverdue() {
        int total = 0;
        int expired;
        do {
            expired = systemAuthRunner.call(() -> expireOverdueUseCase.expireOverdue(batchSize));
            total += expired;
        } while (expired == batchSize);

        if (total == 0)
            return Outcome.NO_WORK;
        log.info("Cotizaciones caducadas por vencimiento: {}", total);
        return Outcome.SUCCESS;
    }
}
