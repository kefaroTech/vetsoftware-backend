package com.vetsoftware.app.diagnosticimaging.application.port.in;

import com.vetsoftware.app.diagnosticimaging.application.dto.DiagnosticImagingDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDiagnosticImagingsUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('diagnosticimaging.read')")
  List<DiagnosticImagingDto> listAll();
}
