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

    public DeleteSurgeryTypeService(
            SurgeryTypeRepository repository,
            SurgeryChildrenQueryPort surgeryChildrenQueryPort) {
        this.repository = repository;
        this.surgeryChildrenQueryPort = surgeryChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new SurgeryTypeNotFoundException(id));
        if (surgeryChildrenQueryPort.existsActiveBySurgeryTypeId(id)) {
            throw new SurgeryTypeHasActiveChildrenException(id, "surgery");
        }
        repository.delete(id);
    }
}
