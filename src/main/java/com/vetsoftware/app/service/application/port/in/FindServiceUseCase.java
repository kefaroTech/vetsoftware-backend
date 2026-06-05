package com.vetsoftware.app.service.application.port.in;

import com.vetsoftware.app.service.application.dto.ServiceDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindServiceUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('service.read')")
    ServiceDto findById(Long id);
}
