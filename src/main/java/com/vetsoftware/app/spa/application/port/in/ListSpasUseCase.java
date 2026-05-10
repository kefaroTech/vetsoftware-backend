package com.vetsoftware.app.spa.application.port.in;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListSpasUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<SpaDto> listAll();
}
