package com.vetsoftware.app.diagnosticimagingtype.application.port.out;

import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import java.util.List;
import java.util.Optional;

public interface DiagnosticImagingTypeRepository {
    DiagnosticImagingType save(DiagnosticImagingType type);
    Optional<DiagnosticImagingType> findById(Long id);
    List<DiagnosticImagingType> findAll();
    void delete(Long id);
}
