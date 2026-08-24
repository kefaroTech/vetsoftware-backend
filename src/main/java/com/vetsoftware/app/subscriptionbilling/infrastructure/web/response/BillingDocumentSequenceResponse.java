package com.vetsoftware.app.subscriptionbilling.infrastructure.web.response;

import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentSequenceDto;
import java.time.LocalDateTime;

/** Una serie del consecutivo interno. */
public record BillingDocumentSequenceResponse(Long id, String prefix, long nextValue,
        LocalDateTime createdDate) {

    public static BillingDocumentSequenceResponse from(BillingDocumentSequenceDto dto) {
        return new BillingDocumentSequenceResponse(dto.id(), dto.prefix(), dto.nextValue(),
                dto.createdDate());
    }
}
