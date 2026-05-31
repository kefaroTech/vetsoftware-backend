package com.vetsoftware.app.hospitalizationprogressnote.application.usecase;

import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.ListHospitalizationProgressNotesByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationProgressNoteRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization_progress_note.list.byHospitalization")
@Service
public class ListHospitalizationProgressNotesByHospitalizationService
        implements ListHospitalizationProgressNotesByHospitalizationUseCase {
    private final HospitalizationProgressNoteRepository repository;

    public ListHospitalizationProgressNotesByHospitalizationService(HospitalizationProgressNoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<HospitalizationProgressNoteDto> listByHospitalization(Long hospitalizationId) {
        return repository.findAllByHospitalizationId(hospitalizationId).stream()
            .map(HospitalizationProgressNoteDto::from).toList();
    }
}
