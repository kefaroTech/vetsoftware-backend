package com.vetsoftware.app.surgerytype.application.usecase;

import com.vetsoftware.app.surgerytype.application.port.in.DeleteSurgeryTypeUseCase;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.SurgeryTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "surgery_type.delete")
@Service
public class DeleteSurgeryTypeService implements DeleteSurgeryTypeUseCase {
    private final SurgeryTypeRepository repository;

    public DeleteSurgeryTypeService(SurgeryTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new SurgeryTypeNotFoundException(id));
        repository.delete(id);
    }
}
