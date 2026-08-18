package com.vetsoftware.app.surgerytype.application.usecase;

import com.vetsoftware.app.surgerytype.application.port.in.DeleteSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryChildrenQueryPort;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeHasActiveChildrenException;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "surgery.type.delete")
@Service
public class DeleteSurgeryTypeService implements DeleteSurgeryTypeUseCase {
    private final SurgeryTypeRepository repository;
    private final SurgeryChildrenQueryPort surgeryChildrenQueryPort;

    public DeleteSurgeryTypeService(SurgeryTypeRepository repository,
            SurgeryChildrenQueryPort surgeryChildrenQueryPort) {
        this.repository = repository;
        this.surgeryChildrenQueryPort = surgeryChildrenQueryPort;
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
                .orElseThrow(() -> new SurgeryTypeNotFoundException(id));
        if (surgeryChildrenQueryPort.existsActiveBySurgeryTypeId(id)) {
            throw new SurgeryTypeHasActiveChildrenException(id, "surgery");
        }
        repository.delete(id);
    }
}
