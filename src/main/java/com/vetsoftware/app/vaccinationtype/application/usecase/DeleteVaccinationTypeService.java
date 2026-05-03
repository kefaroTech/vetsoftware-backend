package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.port.in.DeleteVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "vaccination_type.delete")
@Service
public class DeleteVaccinationTypeService implements DeleteVaccinationTypeUseCase {
    private final VaccinationTypeRepository repository;

    public DeleteVaccinationTypeService(VaccinationTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new VaccinationTypeNotFoundException(id));
        repository.delete(id);
    }
}
