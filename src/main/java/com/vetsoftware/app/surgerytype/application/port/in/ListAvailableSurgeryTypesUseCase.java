package com.vetsoftware.app.surgerytype.application.port.in;

import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListAvailableSurgeryTypesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or @authz.isMyCompany(#companyId)")
    List<SurgeryTypeDto> listAvailable(Long companyId);
}
