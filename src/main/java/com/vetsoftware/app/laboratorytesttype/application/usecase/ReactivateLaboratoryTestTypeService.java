package com.vetsoftware.app.laboratorytesttype.application.usecase;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.in.ReactivateLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "laboratory.test.type.reactivate")
@Service
public class ReactivateLaboratoryTestTypeService implements ReactivateLaboratoryTestTypeUseCase {
    private final LaboratoryTestTypeRepository repository;

    public ReactivateLaboratoryTestTypeService(LaboratoryTestTypeRepository repository) {
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
    public LaboratoryTestTypeDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new LaboratoryTestTypeNotFoundException(id);
        return LaboratoryTestTypeDto.from(repository.findOwnedByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new LaboratoryTestTypeNotFoundException(id)));
    }
}
