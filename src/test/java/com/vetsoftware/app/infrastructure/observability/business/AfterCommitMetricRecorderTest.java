package com.vetsoftware.app.infrastructure.observability.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

class AfterCommitMetricRecorderTest {

    private final AfterCommitMetricRecorder recorder = new AfterCommitMetricRecorder();

    @AfterEach
    void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void recordsOnlyAfterCommitWhenTransactionIsActive() {
        AtomicInteger value = new AtomicInteger();
        beginTransactionSynchronization();

        recorder.recordAfterCommit(value::incrementAndGet);

        assertThat(value).hasValue(0);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertThat(value).hasValue(1);
    }

    @Test
    void doesNotRecordSuccessAfterRollback() {
        AtomicInteger value = new AtomicInteger();
        beginTransactionSynchronization();

        recorder.recordAfterCommit(value::incrementAndGet);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization
                        .afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(value).hasValue(0);
    }

    @Test
    void recordsImmediatelyWithoutTransaction() {
        AtomicInteger value = new AtomicInteger();

        recorder.recordAfterCommit(value::incrementAndGet);

        assertThat(value).hasValue(1);
    }

    @Test
    @DisplayName("recordNow ejecuta la acción de inmediato incluso con una transacción activa")
    void recordNowRunsImmediatelyEvenWithAnActiveTransaction() {
        AtomicInteger value = new AtomicInteger();
        beginTransactionSynchronization();

        recorder.recordNow(value::incrementAndGet);

        assertThat(value).hasValue(1);
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    @DisplayName("un fallo al registrar la métrica se registra en el log y no se propaga")
    void swallowsExceptionsRaisedByTheMetricAction() {
        assertThatCode(() -> recorder.recordNow(() -> {
            throw new IllegalStateException("registro no disponible");
        })).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("una acción nula en recordNow se rechaza explícitamente")
    void recordNowRejectsANullAction() {
        assertThatThrownBy(() -> recorder.recordNow(null)).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("action");
    }

    @Test
    @DisplayName("una acción nula en recordAfterCommit se rechaza explícitamente")
    void recordAfterCommitRejectsANullAction() {
        assertThatThrownBy(() -> recorder.recordAfterCommit(null))
                .isInstanceOf(NullPointerException.class).hasMessageContaining("action");
    }

    @Test
    @DisplayName("una métrica registrada desde dentro de un afterCommit se publica igualmente")
    void publishesAMetricRecordedFromInsideAnAfterCommitCallback() {
        AtomicInteger value = new AtomicInteger();
        beginTransactionSynchronization();
        // Patrón A1 del repositorio: el caso de uso difiere el trabajo caro a un
        // afterCommit y la métrica de negocio se publica desde ahí dentro, con las
        // dos banderas del manager todavía en true. Es el camino de
        // EmitElectronicDocumentOnCloseService -> ClosedAccountEmissionCompleter.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                recorder.recordAfterCommit(value::incrementAndGet);
            }
        });

        triggerCommitSequence();

        assertThat(value).hasValue(1);
    }

    @Test
    @DisplayName("la métrica registrada tarde y la registrada a tiempo se publican una sola vez")
    void publishesBothTheEarlyAndTheLateMetricExactlyOnce() {
        AtomicInteger early = new AtomicInteger();
        AtomicInteger late = new AtomicInteger();
        beginTransactionSynchronization();
        recorder.recordAfterCommit(early::incrementAndGet);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                recorder.recordAfterCommit(late::incrementAndGet);
            }
        });

        triggerCommitSequence();

        // early: afterCommit la publicó y el barrido de afterCompletion no puede
        // volver a contarla. late: afterCommit nunca la vio y afterCompletion la
        // rescata.
        assertThat(early).hasValue(1);
        assertThat(late).hasValue(1);
    }

    @Test
    @DisplayName("con desenlace desconocido la métrica se descarta en vez de publicarse")
    void doesNotPublishWhenTheOutcomeIsUnknown() {
        AtomicInteger value = new AtomicInteger();
        beginTransactionSynchronization();

        recorder.recordAfterCommit(value::incrementAndGet);
        TransactionSynchronizationUtils
                .triggerAfterCompletion(TransactionSynchronization.STATUS_UNKNOWN);

        assertThat(value).hasValue(0);
    }

    private static void beginTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    /**
     * Reproduce el orden real de
     * {@code AbstractPlatformTransactionManager.processCommit} usando los mismos
     * helpers que él delega: {@code triggerAfterCommit()} itera el snapshot que
     * {@code getSynchronizations()} devolvió al empezar, y
     * {@code triggerAfterCompletion(int)} vuelve a pedirlo —por eso sí ve a las
     * registradas durante la primera fase—. La limpieza va al final, en el
     * {@code finally} externo.
     */
    private static void triggerCommitSequence() {
        TransactionSynchronizationUtils.triggerAfterCommit();
        TransactionSynchronizationUtils
                .triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }
}
