package com.vetsoftware.app.subscriptionbilling.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionbilling.application.dto.BillingDocumentSequenceDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Las series del consecutivo interno. Contador global: solo SYSTEM. */
public interface ListBillingDocumentSequencesUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<BillingDocumentSequenceDto> listAll(int page, int pageSize);
}
