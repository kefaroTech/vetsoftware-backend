package com.vetsoftware.app.diagnosticimagingtype.application.port.out;

import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import java.util.List;
import java.util.Optional;

public interface DiagnosticImagingTypeRepository {
  DiagnosticImagingType save(DiagnosticImagingType type);

  Optional<DiagnosticImagingType> findById(Long id);

  Optional<DiagnosticImagingType> findByIdAndCompanyId(Long id, Long companyId);

  List<DiagnosticImagingType> findAll();

  List<DiagnosticImagingType> findAllAvailableForCompany(Long companyId);

  void delete(Long id);

  int reactivate(Long id);
}
