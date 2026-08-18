package com.vetsoftware.app.vaccination.application.usecase;

import com.vetsoftware.app.vaccination.application.port.in.DeleteVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.domain.VaccinationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "vaccination.delete")
@Service
public class DeleteVaccinationService implements DeleteVaccinationUseCase {
    private final VaccinationRepository repository;

    public DeleteVaccinationService(VaccinationRepository repository) {
        this.repository = repository;
    }

    /**
     * La comprobacion previa de existencia es la que decide si se borra, asi que
     * tiene que ser por (id, empresa): con un findById a secas, la vacuna de otro
     * tenant existia, pasaba la guarda y se borraba.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new VaccinationNotFoundException(id));
        repository.delete(id);
    }
}
