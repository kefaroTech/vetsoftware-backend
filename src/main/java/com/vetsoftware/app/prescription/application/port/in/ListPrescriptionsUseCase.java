package com.vetsoftware.app.prescription.application.port.in;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPrescriptionsUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<PrescriptionDto> listAll(int page, int pageSize);
}
