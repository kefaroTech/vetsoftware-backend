package com.vetsoftware.app.consultationtype.application.port.in;

import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListConsultationTypesUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<ConsultationTypeDto> listAll();
}
