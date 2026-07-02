package com.vetsoftware.app.service.application.port.in;

import com.vetsoftware.app.service.application.dto.ServiceDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListServicesByCompanyUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('service.read') and @authz.isMyCompany(#companyId))")
    List<ServiceDto> listByCompany(Long companyId);

    /** Lista los servicios PAUSADOS (enabled=false) de la empresa, para el flujo de reactivación. */
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('service.read') and @authz.isMyCompany(#companyId))")
    List<ServiceDto> listDisabledByCompany(Long companyId);
}
