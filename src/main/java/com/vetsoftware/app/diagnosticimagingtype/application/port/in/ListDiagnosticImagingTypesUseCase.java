package com.vetsoftware.app.diagnosticimagingtype.application.port.in;

import com.vetsoftware.app.diagnosticimagingtype.application.dto.DiagnosticImagingTypeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListDiagnosticImagingTypesUseCase {
  @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('diagnosticimaging.read'))")
  List<DiagnosticImagingTypeDto> listAll();
}
