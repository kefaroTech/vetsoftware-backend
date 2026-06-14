package com.vetsoftware.app.numberingresolution.application.port.in;

import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListNumberingResolutionsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('numberingResolution.read') and @authz.isMyCompany(#companyId))")
    List<NumberingResolutionDto> listByCompany(Long companyId);
}
