package com.vetsoftware.app.infrastructure.observability.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
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
        .forEach(
            synchronization ->
                synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

    assertThat(value).hasValue(0);
  }

  @Test
  void recordsImmediatelyWithoutTransaction() {
    AtomicInteger value = new AtomicInteger();

    recorder.recordAfterCommit(value::incrementAndGet);

    assertThat(value).hasValue(1);
  }

  private static void beginTransactionSynchronization() {
    TransactionSynchronizationManager.setActualTransactionActive(true);
    TransactionSynchronizationManager.initSynchronization();
  }
}
