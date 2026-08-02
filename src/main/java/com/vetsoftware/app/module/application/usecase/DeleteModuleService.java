package com.vetsoftware.app.module.application.usecase;

import com.vetsoftware.app.module.application.port.in.DeleteModuleUseCase;
import com.vetsoftware.app.module.application.port.out.ModuleRepository;
import com.vetsoftware.app.module.application.port.out.SubModuleChildrenQueryPort;
import com.vetsoftware.app.module.domain.ModuleHasActiveChildrenException;
import com.vetsoftware.app.module.domain.ModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "module.delete")
@Service
public class DeleteModuleService implements DeleteModuleUseCase {
    private final ModuleRepository repository;
    private final SubModuleChildrenQueryPort subModuleChildrenQueryPort;

    public DeleteModuleService(ModuleRepository repository,
            SubModuleChildrenQueryPort subModuleChildrenQueryPort) {
        this.repository = repository;
        this.subModuleChildrenQueryPort = subModuleChildrenQueryPort;
    }

    @Override
    @Transactional
    public void execute(Long id) {
        repository.findById(id).orElseThrow(() -> new ModuleNotFoundException(id));
        if (subModuleChildrenQueryPort.existsActiveByModuleId(id)) {
            throw new ModuleHasActiveChildrenException(id, "subModule");
        }
        repository.delete(id);
    }
}
