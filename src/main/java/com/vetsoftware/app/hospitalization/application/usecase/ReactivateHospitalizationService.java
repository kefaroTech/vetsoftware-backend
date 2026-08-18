package com.vetsoftware.app.hospitalization.application.usecase;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.port.in.ReactivateHospitalizationUseCase;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import com.vetsoftware.app.hospitalization.domain.HospitalizationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.reactivate")
@Service
public class ReactivateHospitalizationService implements ReactivateHospitalizationUseCase {
    private final HospitalizationRepository repository;

    public ReactivateHospitalizationService(HospitalizationRepository repository) {
        this.repository = repository;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aqui no hay un
     * findById previo que valide la propiedad, asi que si la consulta no filtra por
     * empresa, un id ajeno se reactiva sin mas. Cero filas afectadas significa «no
     * existe en TU empresa», que es tambien la respuesta correcta para la
     * hospitalizacion de otro tenant: un 404, sin revelar que el id existe.
     */
    @Override
    @Transactional
    public HospitalizationDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new HospitalizationNotFoundException(id);
        return HospitalizationDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new HospitalizationNotFoundException(id)));
    }
}
