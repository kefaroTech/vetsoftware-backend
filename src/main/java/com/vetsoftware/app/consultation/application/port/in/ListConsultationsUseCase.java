package com.vetsoftware.app.consultation.application.port.in;

import com.vetsoftware.app.consultation.application.dto.ConsultationDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListConsultationsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<ConsultationDto> listAll(Long companyId, int page, int pageSize);
}
