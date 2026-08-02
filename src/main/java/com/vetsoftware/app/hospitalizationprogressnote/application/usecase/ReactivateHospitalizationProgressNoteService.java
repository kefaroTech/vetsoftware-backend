package com.vetsoftware.app.hospitalizationprogressnote.application.usecase;

import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.ReactivateHospitalizationProgressNoteUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationProgressNoteRepository;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNoteNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.progress.note.reactivate")
@Service
public class ReactivateHospitalizationProgressNoteService
        implements
            ReactivateHospitalizationProgressNoteUseCase {
    private final HospitalizationProgressNoteRepository repository;

    public ReactivateHospitalizationProgressNoteService(
            HospitalizationProgressNoteRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public HospitalizationProgressNoteDto execute(Long id) {
        int updated = repository.reactivate(id);
        if (updated == 0)
            throw new HospitalizationProgressNoteNotFoundException(id);
        return HospitalizationProgressNoteDto.from(repository.findById(id)
                .orElseThrow(() -> new HospitalizationProgressNoteNotFoundException(id)));
    }
}
