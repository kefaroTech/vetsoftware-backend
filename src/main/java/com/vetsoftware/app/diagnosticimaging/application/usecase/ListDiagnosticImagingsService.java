package com.vetsoftware.app.diagnosticimaging.application.usecase;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import com.vetsoftware.app.diagnosticimaging.application.port.in.ListDiagnosticImagingsUseCase;
import com.vetsoftware.app.diagnosticimaging.application.port.out.DiagnosticImagingRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "diagnostic.imaging.list")
@Service
public class ListDiagnosticImagingsService implements ListDiagnosticImagingsUseCase {
  private final DiagnosticImagingRepository repository;

  public ListDiagnosticImagingsService(DiagnosticImagingRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<DiagnosticImagingDto> listAll() {
    return repository.findAll().stream().map(DiagnosticImagingDto::from).toList();
  }
}
