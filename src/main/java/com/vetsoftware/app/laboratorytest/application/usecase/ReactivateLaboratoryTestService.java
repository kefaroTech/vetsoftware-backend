package com.vetsoftware.app.laboratorytest.application.usecase;

import com.vetsoftware.app.laboratorytest.application.dto.LaboratoryTestDto;
import com.vetsoftware.app.laboratorytest.application.port.in.ReactivateLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "laboratory.test.reactivate")
@Service
public class ReactivateLaboratoryTestService implements ReactivateLaboratoryTestUseCase {
    private final LaboratoryTestRepository repository;

    public ReactivateLaboratoryTestService(LaboratoryTestRepository repository) {
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
    public LaboratoryTestDto execute(Long id, Long companyId) {
        int rows = repository.reactivate(id, companyId);
        if (rows == 0)
            throw new LaboratoryTestNotFoundException(id);
        return LaboratoryTestDto.from(repository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new LaboratoryTestNotFoundException(id)));
    }
}
