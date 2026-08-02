package com.vetsoftware.app.hospitalizationprogressnote.application.usecase;

import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.FindHospitalizationProgressNoteUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationProgressNoteRepository;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNoteNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.progress.note.find")
@Service
public class FindHospitalizationProgressNoteService
        implements
            FindHospitalizationProgressNoteUseCase {
    private final HospitalizationProgressNoteRepository repository;

    public FindHospitalizationProgressNoteService(
            HospitalizationProgressNoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public HospitalizationProgressNoteDto findById(Long id, Long companyId) {
        return HospitalizationProgressNoteDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new HospitalizationProgressNoteNotFoundException(id)));
    }
}
