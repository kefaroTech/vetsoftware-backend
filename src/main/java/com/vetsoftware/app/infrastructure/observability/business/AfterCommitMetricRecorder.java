package com.vetsoftware.app.infrastructure.observability.business;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Evita publicar éxitos de negocio antes de confirmar la transacción. Las
 * métricas son best-effort: un fallo del registro nunca debe cambiar el
 * resultado del caso de uso.
 */
@Component
public class AfterCommitMetricRecorder {

    private static final Logger log = LoggerFactory.getLogger(AfterCommitMetricRecorder.class);

    public void recordAfterCommit(Runnable action) {
        Objects.requireNonNull(action, "action es obligatoria");
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager
                    .registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            safelyRun(action);
                        }
                    });
            return;
        }
        safelyRun(action);
    }

    public void recordNow(Runnable action) {
        Objects.requireNonNull(action, "action es obligatoria");
        safelyRun(action);
    }

    private static void safelyRun(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            // El throwable como último argumento y no getMessage(): una NPE trae
            // mensaje null y getMessage() se lleva por delante la cadena de causas
            // y el stacktrace, que es lo único que identifica al llamador.
            log.warn("No se pudo registrar una métrica de negocio", exception);
        }
    }
}
