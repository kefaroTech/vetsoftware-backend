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

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aqui no hay un
     * findById previo que valide la propiedad, asi que si la consulta no filtra por
     * empresa, un id ajeno se reactiva sin mas. Cero filas afectadas significa «no
     * existe en TU empresa», que es tambien la respuesta correcta para la nota de
     * otro tenant: un 404, sin revelar que el id existe.
     */
    @Override
    @Transactional
    public HospitalizationProgressNoteDto execute(Long id, Long companyId) {
        int updated = repository.reactivate(id, companyId);
        if (updated == 0)
            throw new HospitalizationProgressNoteNotFoundException(id);
        return HospitalizationProgressNoteDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new HospitalizationProgressNoteNotFoundException(id)));
    }
}
