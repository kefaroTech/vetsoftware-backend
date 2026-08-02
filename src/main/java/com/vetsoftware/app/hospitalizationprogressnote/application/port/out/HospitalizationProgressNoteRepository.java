package com.vetsoftware.app.hospitalizationprogressnote.application.port.out;

import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
import java.util.List;
import java.util.Optional;

public interface HospitalizationProgressNoteRepository {
  HospitalizationProgressNote save(HospitalizationProgressNote progressNote);

  Optional<HospitalizationProgressNote> findById(Long id);

  Optional<HospitalizationProgressNote> findByIdAndCompanyId(Long id, Long companyId);

  List<HospitalizationProgressNote> findAllByHospitalizationId(Long hospitalizationId);

  void delete(Long id);

  int reactivate(Long id);
}
