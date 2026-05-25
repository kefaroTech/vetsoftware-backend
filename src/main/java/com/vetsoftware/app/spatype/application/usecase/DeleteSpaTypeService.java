package com.vetsoftware.app.spatype.application.usecase;

import com.vetsoftware.app.spatype.application.port.in.DeleteSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.out.SpaChildrenQueryPort;
import com.vetsoftware.app.spatype.application.port.out.SpaTypeRepository;
import com.vetsoftware.app.spatype.domain.SpaTypeHasActiveChildrenException;
import com.vetsoftware.app.spatype.domain.SpaTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "spa_type.delete")
@Service
public class DeleteSpaTypeService implements DeleteSpaTypeUseCase {
    private final SpaTypeRepository repository;
    private final SpaChildrenQueryPort spaChildrenQueryPort;

    public DeleteSpaTypeService(
            SpaTypeRepository repository,
            SpaChildrenQueryPort spaChildrenQueryPort) {
        this.repository = repository;
        this.spaChildrenQueryPort = spaChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new SpaTypeNotFoundException(id));
        if (spaChildrenQueryPort.existsActiveBySpaTypeId(id)) {
            throw new SpaTypeHasActiveChildrenException(id, "spa");
        }
        repository.delete(id);
    }
}
