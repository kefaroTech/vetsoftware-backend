package com.vetsoftware.app.hospitalizationprogressnote.application.usecase;

import com.vetsoftware.app.hospitalizationprogressnote.application.command.UpdateHospitalizationProgressNoteCommand;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.UpdateHospitalizationProgressNoteUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationProgressNoteRepository;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNoteNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization_progress_note.update")
@Service
public class UpdateHospitalizationProgressNoteService implements UpdateHospitalizationProgressNoteUseCase {
    private final HospitalizationProgressNoteRepository repository;

    public UpdateHospitalizationProgressNoteService(HospitalizationProgressNoteRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public HospitalizationProgressNoteDto execute(UpdateHospitalizationProgressNoteCommand command) {
        HospitalizationProgressNote progressNote = repository.findById(command.id())
            .orElseThrow(() -> new HospitalizationProgressNoteNotFoundException(command.id()));
        progressNote.update(command.description());
        return HospitalizationProgressNoteDto.from(repository.save(progressNote));
    }
}
