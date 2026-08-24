package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.application.port.out.SubModuleQueryPort;
import com.vetsoftware.app.catalogitem.domain.SubModuleRef;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * El único archivo de esta feature —junto a las dos clases de persistencia del
 * puente— que conoce la feature {@code submodule}, y lo hace por la excepción
 * acotada del {@code CLAUDE.md}: {@code infrastructure/persistence} puede
 * importar el {@code XxxJpaRepository} y el {@code XxxJpaEntity} de otra
 * feature, nunca su dominio ni sus DTOs.
 */
@Component
public class JpaSubModuleQueryPort implements SubModuleQueryPort {

    private final SubModuleJpaRepository subModuleJpaRepository;

    public JpaSubModuleQueryPort(SubModuleJpaRepository subModuleJpaRepository) {
        this.subModuleJpaRepository = subModuleJpaRepository;
    }

    @Override
    public Optional<SubModuleRef> findById(Long subModuleId) {
        return subModuleJpaRepository.findById(subModuleId).map(
                entity -> new SubModuleRef(entity.getId(), entity.getName(), entity.getCode()));
    }
}
