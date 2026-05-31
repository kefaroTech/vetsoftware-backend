package com.vetsoftware.app.laboratorytestfile.application.port.in;

import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDownloadDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface DownloadLaboratoryTestFileUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('laboratoryTest.read') or hasRole('SYSTEM')")
    LaboratoryTestFileDownloadDto download(Long id);
}
