package com.vetsoftware.app.prescription.application.port.in;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListPrescriptionsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasRole('SYSTEM')")
    List<PrescriptionDto> listAll();
}
