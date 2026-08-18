package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.ReactivateHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.observation.reactivate")
@Service
public class ReactivateHospitalizationObservationService
        implements
            ReactivateHospitalizationObservationUseCase {
    private final HospitalizationObservationRepository repository;

    public ReactivateHospitalizationObservationService(
            HospitalizationObservationRepository repository) {
        this.repository = repository;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aqui no hay un
     * findById previo que valide la propiedad, asi que si la consulta no filtra por
     * empresa, un id ajeno se reactiva sin mas. Cero filas afectadas significa «no
     * existe en TU empresa», que es tambien la respuesta correcta para la
     * observacion de otro tenant: un 404, sin revelar que el id existe.
     */
    @Override
    @Transactional
    public HospitalizationObservationDto execute(Long id, Long companyId) {
        int updated = repository.reactivate(id, companyId);
        if (updated == 0)
            throw new HospitalizationObservationNotFoundException(id);
        return HospitalizationObservationDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new HospitalizationObservationNotFoundException(id)));
    }
}
