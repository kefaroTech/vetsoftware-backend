package com.vetsoftware.app.uvtvalue.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.uvtvalue.application.dto.UvtValueDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListUvtValuesUseCase {

    @PreAuthorize("hasRole('SYSTEM') or "
            + "(hasAuthority('uvt.read') and @authz.isMyCompany(#companyId))")
    PageResult<UvtValueDto> listAll(Long companyId, int page, int pageSize);
}
