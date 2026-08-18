package com.vetsoftware.app.laboratorytest.application.usecase;

import com.vetsoftware.app.laboratorytest.application.port.in.DeleteLaboratoryTestUseCase;
import com.vetsoftware.app.laboratorytest.application.port.out.LaboratoryTestRepository;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "laboratory.test.delete")
@Service
public class DeleteLaboratoryTestService implements DeleteLaboratoryTestUseCase {
    private final LaboratoryTestRepository repository;

    public DeleteLaboratoryTestService(LaboratoryTestRepository repository) {
        this.repository = repository;
    }

    /**
     * La lectura previa acotada por empresa es lo que convierte un id ajeno en un
     * 404 en vez de en un borrado. Un {@code companyId} nulo es el actor global
     * (SYSTEM), que si puede borrar cualquier fila.
     */
    @Override
    @Transactional
    public void execute(Long id, Long companyId) {
        (companyId == null
                ? repository.findById(id)
                : repository.findByIdAndCompanyId(id, companyId))
                .orElseThrow(() -> new LaboratoryTestNotFoundException(id));
        repository.delete(id);
    }
}
