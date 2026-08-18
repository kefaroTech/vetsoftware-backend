package com.vetsoftware.app.vaccinationtype.application.usecase;

import com.vetsoftware.app.vaccinationtype.application.dto.VaccinationTypeDto;
import com.vetsoftware.app.vaccinationtype.application.port.in.ReactivateVaccinationTypeUseCase;
import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "vaccination.type.reactivate")
@Service
public class ReactivateVaccinationTypeService implements ReactivateVaccinationTypeUseCase {
    private final VaccinationTypeRepository repository;

    public ReactivateVaccinationTypeService(VaccinationTypeRepository repository) {
        this.repository = repository;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aquí no hay un
     * findById previo que valide la propiedad, así que sin filtrar por empresa se
     * revivía el tipo de otro tenant. La relectura usa el finder ESTRICTO —lo
     * reactivado es siempre propio— y no el de disponibles, que incluye las
     * generales.
     */
    @Override
    @Transactional
    public VaccinationTypeDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new VaccinationTypeNotFoundException(id);
        return VaccinationTypeDto.from(repository.findOwnedByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new VaccinationTypeNotFoundException(id)));
    }
}
