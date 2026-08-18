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

    /**
     * {@code companyId} null = caller sin empresa (SYSTEM), único que puede borrar
     * una fila general. Con empresa, la lectura previa va al finder ESTRICTO: el
     * tipo de otro tenant y el general compartido son ambos un 404, no un borrado.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        (companyId == null
                ? repository.findById(id)
                : repository.findOwnedByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new VaccinationTypeNotFoundException(id));
        if (vaccinationChildrenQueryPort.existsActiveByVaccinationTypeId(id)) {
            throw new VaccinationTypeHasActiveChildrenException(id, "vaccination");
        }
        repository.delete(id);
    }
}
