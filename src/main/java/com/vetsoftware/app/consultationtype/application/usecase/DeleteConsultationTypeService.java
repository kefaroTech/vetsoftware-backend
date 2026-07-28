package com.vetsoftware.app.consultationtype.application.usecase;

import com.vetsoftware.app.consultationtype.application.port.in.DeleteConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationChildrenQueryPort;
import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeHasActiveChildrenException;
import com.vetsoftware.app.consultationtype.domain.ConsultationTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "consultation.type.delete")
@Service
public class DeleteConsultationTypeService implements DeleteConsultationTypeUseCase {
    private final ConsultationTypeRepository repository;
    private final ConsultationChildrenQueryPort consultationChildrenQueryPort;

    public DeleteConsultationTypeService(
            ConsultationTypeRepository repository,
            ConsultationChildrenQueryPort consultationChildrenQueryPort) {
        this.repository = repository;
        this.consultationChildrenQueryPort = consultationChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new ConsultationTypeNotFoundException(id));
        if (consultationChildrenQueryPort.existsActiveByConsultationTypeId(id)) {
            throw new ConsultationTypeHasActiveChildrenException(id, "consultation");
        }
        repository.delete(id);
    }
}
