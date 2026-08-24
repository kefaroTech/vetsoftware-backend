package com.vetsoftware.app.quote.application.port.in;

import com.vetsoftware.app.quote.application.dto.QuoteSummaryDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/** Las cotizaciones de UNA empresa: lo que ve el cliente en su consola. */
public interface ListQuotesByCompanyUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('quote.read') "
            + "and @authz.isMyCompany(#companyId))")
    PageResult<QuoteSummaryDto> listByCompany(Long companyId, int page, int pageSize);
}
