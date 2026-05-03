package com.vetsoftware.app.hospitalization.application.port.in;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListHospitalizationsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<HospitalizationDto> listAll();
}
