package com.vetsoftware.app.submodule.infrastructure.persistence;

import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaEntity;
import com.vetsoftware.app.module.infrastructure.persistence.ModuleJpaRepository;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.domain.SubModule;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSubModuleRepository implements SubModuleRepository {
    private final SubModuleJpaRepository jpaRepository;
    private final SubModuleJpaMapper mapper;
    private final ModuleJpaRepository moduleJpaRepository;

    public JpaSubModuleRepository(SubModuleJpaRepository jpaRepository,
                                   SubModuleJpaMapper mapper,
                                   ModuleJpaRepository moduleJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.moduleJpaRepository = moduleJpaRepository;
    }

    @Override
    public SubModule save(SubModule subModule) {
        ModuleJpaEntity module = moduleJpaRepository.getReferenceById(subModule.getModule().id());
        SubModuleJpaEntity saved = jpaRepository.save(mapper.toJpa(subModule, module));
        return mapper.toDomain(saved, subModule.getModule());
    }

    @Override
    public Optional<SubModule> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SubModule> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
