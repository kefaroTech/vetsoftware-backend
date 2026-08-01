package com.vetsoftware.app.consultation.application.port.in;

import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListConsultationsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    List<ConsultationDto> listAll(Long companyId);
}
