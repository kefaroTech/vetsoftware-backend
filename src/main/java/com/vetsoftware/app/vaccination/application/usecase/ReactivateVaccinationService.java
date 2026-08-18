package com.vetsoftware.app.vaccination.application.usecase;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.port.in.ReactivateVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.domain.VaccinationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "vaccination.reactivate")
@Service
public class ReactivateVaccinationService implements ReactivateVaccinationUseCase {
    private final VaccinationRepository repository;

    public ReactivateVaccinationService(VaccinationRepository repository) {
        this.repository = repository;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aqui no hay un
     * findById previo que valide la propiedad, asi que si la consulta no filtra por
     * empresa, un id ajeno se reactiva sin mas. Cero filas afectadas significa «no
     * existe en TU empresa», que es tambien la respuesta correcta para el registro
     * de otro tenant: un 404, sin revelar que el id existe.
     */
    @Override
    @Transactional
    public VaccinationDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new VaccinationNotFoundException(id);
        return VaccinationDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new VaccinationNotFoundException(id)));
    }
}
