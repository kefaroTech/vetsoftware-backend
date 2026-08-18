package com.vetsoftware.app.numberingresolution.application.usecase;

import com.vetsoftware.app.numberingresolution.application.dto.NumberingResolutionDto;
import com.vetsoftware.app.numberingresolution.application.port.in.ReactivateNumberingResolutionUseCase;
import com.vetsoftware.app.numberingresolution.application.port.out.NumberingResolutionRepository;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolutionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "numbering.resolution.reactivate")
@Service
public class ReactivateNumberingResolutionService implements ReactivateNumberingResolutionUseCase {
    private final NumberingResolutionRepository repository;

    public ReactivateNumberingResolutionService(NumberingResolutionRepository repository) {
        this.repository = repository;
    }

    /**
     * La empresa viaja hasta el UPDATE y hasta la relectura: aquí no hay un
     * findById previo que valide la propiedad, así que si la consulta no filtra por
     * empresa se revive la numeración fiscal de otro tenant. Cero filas afectadas
     * significa «no existe en TU empresa» y se responde 404, sin revelar que el id
     * existe.
     */
    @Override
    @Transactional
    public NumberingResolutionDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new NumberingResolutionNotFoundException(id);
        return NumberingResolutionDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NumberingResolutionNotFoundException(id)));
    }
}
