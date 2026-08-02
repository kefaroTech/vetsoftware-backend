package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.port.in.DeleteVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationChildrenQueryPort;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeHasActiveChildrenException;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "vaccination.type.delete")
@Service
public class DeleteVaccinationTypeService implements DeleteVaccinationTypeUseCase {
    private final VaccinationTypeRepository repository;
    private final VaccinationChildrenQueryPort vaccinationChildrenQueryPort;

    public DeleteVaccinationTypeService(VaccinationTypeRepository repository,
            VaccinationChildrenQueryPort vaccinationChildrenQueryPort) {
        this.repository = repository;
        this.vaccinationChildrenQueryPort = vaccinationChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new VaccinationTypeNotFoundException(id));
        if (vaccinationChildrenQueryPort.existsActiveByVaccinationTypeId(id)) {
            throw new VaccinationTypeHasActiveChildrenException(id, "vaccination");
        }
        repository.delete(id);
    }
}
