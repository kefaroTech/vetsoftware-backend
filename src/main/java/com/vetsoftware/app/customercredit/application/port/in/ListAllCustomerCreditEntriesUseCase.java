package com.vetsoftware.app.customercredit.application.port.in;

import com.vetsoftware.app.customercredit.application.dto.CustomerCreditEntryDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/** Consulta cross-tenant del libro para la consola de plataforma. */
public interface ListAllCustomerCreditEntriesUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<CustomerCreditEntryDto> listAll(Long companyId, int page, int pageSize);
}
