package com.vetsoftware.app.hospitalizationprogressnote.application.port.out;

import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.PageResult;
import java.util.Optional;

public interface HospitalizationProgressNoteRepository {
    HospitalizationProgressNote save(HospitalizationProgressNote progressNote);

    Optional<HospitalizationProgressNote> findById(Long id);

    Optional<HospitalizationProgressNote> findByIdAndCompanyId(Long id, Long companyId);

    PageResult<HospitalizationProgressNote> findAllByHospitalizationId(Long hospitalizationId,
            int page, int pageSize);

    void delete(Long id);

    int reactivate(Long id);
}
