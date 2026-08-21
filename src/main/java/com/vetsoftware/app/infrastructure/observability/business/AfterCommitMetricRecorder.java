package com.vetsoftware.app.infrastructure.observability.business;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Evita publicar éxitos de negocio antes de confirmar la transacción. Las
 * métricas son best-effort: un fallo del registro nunca debe cambiar el
 * resultado del caso de uso.
 *
 * <p>
 * <b>Best-effort no significa «se puede perder callado».</b> La sincronización
 * que registra {@link #recordAfterCommit(Runnable)} atiende las DOS fases del
 * cierre —{@code afterCommit} y {@code afterCompletion}— y toda ruta que no
 * publique la acción deja rastro en el log. El motivo es el orden real de
 * {@code AbstractPlatformTransactionManager.processCommit}:
 *
 * <ol>
 * <li>{@code triggerAfterCommit(status)} — dentro del {@code try}. Itera el
 * <i>snapshot</i> que {@code TransactionSynchronizationManager
 * .getSynchronizations()} devolvió al empezar
 * ({@code new ArrayList<>(synchs)}), precisamente para tolerar que un callback
 * registre más sincronizaciones. Las que se registran <i>durante</i> esta fase
 * NO reciben {@code afterCommit}: no están en la copia que se está
 * recorriendo.</li>
 * <li>{@code triggerAfterCompletion(status)} — vuelve a pedir
 * {@code getSynchronizations()}, así que este segundo barrido SÍ incluye a las
 * registradas tarde. Es el único punto en el que esa métrica todavía puede
 * publicarse.</li>
 * <li>{@code cleanupAfterCompletion(status)} — {@code finally} externo, el
 * único que llama a {@code clear()}.</li>
 * </ol>
 *
 * <p>
 * De ahí que las dos banderas que decide {@link #recordAfterCommit(Runnable)}
 * sigan valiendo {@code true} dentro de un callback {@code afterCommit}
 * ({@code isActualTransactionActive()} no se limpia hasta el paso 3;
 * {@code isSynchronizationActive()}, hasta el 2), y que la llamada tome la rama
 * de {@code registerSynchronization} en lugar de la inmediata. Es el patrón A1
 * del repositorio —diferir el I/O caro a {@code afterCommit}— así que cualquier
 * caso de uso que lo siga publica sus métricas desde dentro de esa fase.
 *
 * <p>
 * Límite conocido: una acción registrada desde dentro de un callback
 * {@code afterCompletion} ajeno tampoco sería invocada, porque el paso 2
 * también itera un snapshot y detrás solo queda el {@code clear()}. Hoy ninguna
 * clase del repositorio publica métricas desde un {@code afterCompletion}.
 */
@Component
public class AfterCommitMetricRecorder {

    private static final Logger log = LoggerFactory.getLogger(AfterCommitMetricRecorder.class);

    public void recordAfterCommit(Runnable action) {
        Objects.requireNonNull(action, "action es obligatoria");
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            // Un guard por registro, no por recorder: el recorder es un singleton
            // compartido por todos los hilos y cada acción tiene su propio ciclo de
            // vida.
            AtomicBoolean pending = new AtomicBoolean(true);
            TransactionSynchronizationManager
                    .registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            runOnce(pending, action);
                        }

                        @Override
                        public void afterCompletion(int status) {
                            afterCompletionFallback(pending, action, status);
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

    /**
     * Segunda —y última— oportunidad de publicar. En el caso normal
     * {@code afterCommit} ya corrió y esto no hace nada: el {@link AtomicBoolean}
     * es lo que impide contar dos veces. Solo llega aquí con la acción pendiente si
     * la sincronización se registró tarde (dentro de la fase {@code afterCommit}) o
     * si la transacción no confirmó.
     *
     * <p>
     * Con desenlace desconocido la acción se descarta a propósito: publicar un
     * éxito de negocio que nadie puede confirmar es peor que no publicarlo. Pero se
     * descarta con un WARN, no en silencio, que es justo lo que hacía imposible
     * distinguir «no pasó nada» de «la métrica se perdió».
     */
    private static void afterCompletionFallback(AtomicBoolean pending, Runnable action,
            int status) {
        if (!pending.get()) {
            return;
        }
        if (status == TransactionSynchronization.STATUS_COMMITTED) {
            if (runOnce(pending, action)) {
                log.debug("Métrica de negocio publicada en afterCompletion: se registró dentro"
                        + " de un callback afterCommit y triggerAfterCommit ya había pasado");
            }
            return;
        }
        if (!pending.compareAndSet(true, false)) {
            return;
        }
        if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
            log.debug("Métrica de negocio descartada: la transacción hizo rollback");
        } else {
            log.warn("Métrica de negocio descartada: la transacción terminó con desenlace"
                    + " desconocido (STATUS_UNKNOWN) y no se publica un éxito sin confirmar");
        }
    }

    /** {@code true} si esta llamada fue la que ejecutó la acción. */
    private static boolean runOnce(AtomicBoolean pending, Runnable action) {
        if (!pending.compareAndSet(true, false)) {
            return false;
        }
        safelyRun(action);
        return true;
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
