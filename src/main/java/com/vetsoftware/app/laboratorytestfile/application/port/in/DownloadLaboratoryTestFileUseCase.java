package com.vetsoftware.app.laboratorytestfile.application.port.in;

import com.vetsoftware.app.laboratorytestfile.application.dto.LaboratoryTestFileDownloadDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface DownloadLaboratoryTestFileUseCase {
    @PreAuthorize("hasRole('SYSTEM') or hasAuthority('laboratoryTest.read')")
    LaboratoryTestFileDownloadDto download(Long id);
}
