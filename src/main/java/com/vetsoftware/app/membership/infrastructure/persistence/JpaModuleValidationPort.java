package com.vetsoftware.app.membership.infrastructure.persistence;

import com.vetsoftware.app.membership.application.port.out.ModuleValidationPort;
import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaEntity;
import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component("membershipJpaModuleValidationPort")
public class JpaModuleValidationPort implements ModuleValidationPort {
    private final ModuleJpaRepository moduleJpaRepository;

    public JpaModuleValidationPort(ModuleJpaRepository moduleJpaRepository) {
        this.moduleJpaRepository = moduleJpaRepository;
    }

    @Override
    public void validateAllExist(List<Long> moduleIds) {
        if (moduleIds == null || moduleIds.isEmpty()) return;
        List<Long> foundIds = moduleJpaRepository.findAllById(moduleIds).stream()
            .map(ModuleJpaEntity::getId)
            .toList();
        List<Long> notFound = moduleIds.stream()
            .filter(id -> !foundIds.contains(id))
            .toList();
        if (!notFound.isEmpty()) {
            throw new IllegalArgumentException("Module IDs not found: " + notFound);
        }
    }
}
