package com.vetsoftware.app.module.application.usecase;

import com.vetsoftware.app.module.application.dto.ModuleDto;
import com.vetsoftware.app.module.application.port.in.ReactivateModuleUseCase;
import com.vetsoftware.app.module.application.port.out.ModuleRepository;
import com.vetsoftware.app.module.domain.ModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "module.reactivate")
@Service
public class ReactivateModuleService implements ReactivateModuleUseCase {
    private final ModuleRepository repository;

    public ReactivateModuleService(ModuleRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public ModuleDto execute(Long id) {
        int rows = repository.reactivate(id);
        if (rows == 0)
            throw new ModuleNotFoundException(id);
        return ModuleDto
                .from(repository.findById(id).orElseThrow(() -> new ModuleNotFoundException(id)));
    }
}
