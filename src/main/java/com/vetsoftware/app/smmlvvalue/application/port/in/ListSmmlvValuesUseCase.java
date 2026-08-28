package com.vetsoftware.app.smmlvvalue.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.smmlvvalue.application.dto.SmmlvValueDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSmmlvValuesUseCase {

    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('smmlv.read') and @authz.isMyCompany(#companyId))")
    PageResult<SmmlvValueDto> listAll(Long companyId, int page, int pageSize);
}
