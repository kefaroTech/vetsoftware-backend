package com.vetsoftware.app.subscriptionbilling.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentSequence;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentNumber;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import(PersistenceSliceConfig.class)
@DisplayName("JpaBillingDocumentSequenceRepository — consecutivo bloqueado contra MySQL real")
class BillingDocumentSequencePersistenceIT extends AbstractDataJpaTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-23T10:00:00Z"),
            ZoneOffset.UTC);

    @Autowired
    private JpaBillingDocumentSequenceRepository repository;

    @Test
    @DisplayName("consume el valor bloqueado e incrementa la serie en la misma transacción")
    void consume_valor_e_incrementa_la_serie() {
        repository.save(BillingDocumentSequence.create("DCT", CLOCK));

        DocumentNumber first = repository.nextNumber("DCT");
        DocumentNumber second = repository.nextNumber("DCT");

        assertThat(first.formatted()).isEqualTo("DCT-000001");
        assertThat(second.formatted()).isEqualTo("DCT-000002");
        assertThat(repository.findByPrefix("DCT")).get()
                .extracting(BillingDocumentSequence::getNextValue).isEqualTo(3L);
    }
}
