package com.vetsoftware.app.diagnosticimaging.application.usecase;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.in.FindDiagnosticImagingUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImagingNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "diagnostic.imaging.find")
@Service
public class FindDiagnosticImagingService implements FindDiagnosticImagingUseCase {
  private final DiagnosticImagingRepository repository;

  public FindDiagnosticImagingService(DiagnosticImagingRepository repository) {
    this.repository = repository;
  }

  @Override
  public DiagnosticImagingDto findById(Long id, Long companyId) {
    return DiagnosticImagingDto.from(
        repository
            .findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new DiagnosticImagingNotFoundException(id)));
  }
}
