package com.vetsoftware.app.hospitalizationprogressnote.application.usecase;

import com.vetsoftware.app.hospitalizationprogressnote.application.dto.HospitalizationProgressNoteDto;
import com.vetsoftware.app.hospitalizationprogressnote.application.dto.PageResult;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.in.ListHospitalizationProgressNotesByHospitalizationUseCase;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationProgressNoteRepository;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.progress.note.list.by.hospitalization")
@Service
public class ListHospitalizationProgressNotesByHospitalizationService
        implements
            ListHospitalizationProgressNotesByHospitalizationUseCase {
    private final HospitalizationProgressNoteRepository repository;

    public ListHospitalizationProgressNotesByHospitalizationService(
            HospitalizationProgressNoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public PageResult<HospitalizationProgressNoteDto> listByHospitalization(Long hospitalizationId,
            int page, int pageSize) {
        return repository.findAllByHospitalizationId(hospitalizationId, page, pageSize)
                .map(HospitalizationProgressNoteDto::from);
    }
}
