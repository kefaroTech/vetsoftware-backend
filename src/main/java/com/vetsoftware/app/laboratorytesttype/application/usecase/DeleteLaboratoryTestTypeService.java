package com.vetsoftware.app.laboratorytesttype.application.usecase;

import com.vetsoftware.app.laboratorytesttype.application.port.in.DeleteLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestChildrenQueryPort;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeHasActiveChildrenException;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "laboratory.test.type.delete")
@Service
public class DeleteLaboratoryTestTypeService implements DeleteLaboratoryTestTypeUseCase {
    private final LaboratoryTestTypeRepository repository;
    private final LaboratoryTestChildrenQueryPort laboratoryTestChildrenQueryPort;

    public DeleteLaboratoryTestTypeService(LaboratoryTestTypeRepository repository,
            LaboratoryTestChildrenQueryPort laboratoryTestChildrenQueryPort) {
        this.repository = repository;
        this.laboratoryTestChildrenQueryPort = laboratoryTestChildrenQueryPort;
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
                .orElseThrow(() -> new LaboratoryTestTypeNotFoundException(id));
        if (laboratoryTestChildrenQueryPort.existsActiveByLaboratoryTestTypeId(id)) {
            throw new LaboratoryTestTypeHasActiveChildrenException(id, "laboratoryTest");
        }
        repository.delete(id);
    }
}
