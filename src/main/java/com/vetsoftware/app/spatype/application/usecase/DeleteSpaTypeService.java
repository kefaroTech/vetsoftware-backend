package com.vetsoftware.app.spatype.application.usecase;

import com.vetsoftware.app.spatype.application.port.in.DeleteSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.out.SpaTypeRepository;
import com.vetsoftware.app.spatype.domain.SpaTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "spa_type.delete")
@Service
public class DeleteSpaTypeService implements DeleteSpaTypeUseCase {
    private final SpaTypeRepository repository;

    public DeleteSpaTypeService(SpaTypeRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new SpaTypeNotFoundException(id));
        repository.delete(id);
    }
}
