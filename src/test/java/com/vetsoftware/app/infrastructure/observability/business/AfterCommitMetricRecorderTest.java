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

    private static void beginTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }
}
