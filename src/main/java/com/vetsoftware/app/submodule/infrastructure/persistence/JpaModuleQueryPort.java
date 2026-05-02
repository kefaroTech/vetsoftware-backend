package com.vetsoftware.app.submodule.infrastructure.persistence;

import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaRepository;
import com.vetsoftware.app.submodule.application.port.out.ModuleQueryPort;
import com.vetsoftware.app.submodule.domain.ModuleRef;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JpaModuleQueryPort implements ModuleQueryPort {
    private final ModuleJpaRepository moduleJpaRepository;

    public JpaModuleQueryPort(ModuleJpaRepository moduleJpaRepository) {
        this.moduleJpaRepository = moduleJpaRepository;
    }

    @Override
    public Optional<ModuleRef> findById(Long moduleId) {
        return moduleJpaRepository.findById(moduleId)
            .map(e -> new ModuleRef(e.getId(), e.getName(), e.getCode()));
    }
}
