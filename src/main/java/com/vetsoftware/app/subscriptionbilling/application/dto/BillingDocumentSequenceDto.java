package com.vetsoftware.app.subscriptionbilling.application.dto;

import com.vetsoftware.app.subscriptionbilling.domain.BillingDocumentSequence;
import java.time.LocalDateTime;

/** Una serie del consecutivo interno. Contador global: sin empresa. */
public record BillingDocumentSequenceDto(Long id, String prefix, long nextValue,
        LocalDateTime createdDate) {

    public static BillingDocumentSequenceDto from(BillingDocumentSequence sequence) {
        return new BillingDocumentSequenceDto(sequence.getId(), sequence.getPrefix(),
                sequence.getNextValue(), sequence.getCreatedDate());
    }
}
