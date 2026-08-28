package com.vetsoftware.app.vatfilingperiod.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.vatfilingperiod.application.dto.VatFilingPeriodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListVatFilingPeriodsUseCase {

    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('vatfiling.read') and @authz.isMyCompany(#companyId))")
    PageResult<VatFilingPeriodDto> listAll(Long companyId, int page, int pageSize);
}
