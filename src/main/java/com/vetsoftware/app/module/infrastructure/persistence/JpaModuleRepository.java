package com.vetsoftware.app.module.infrastructure.persistence;

import com.vetsoftware.app.module.application.port.out.ModuleRepository;
import com.vetsoftware.app.module.domain.Module;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaModuleRepository implements ModuleRepository {
  private final ModuleJpaRepository jpaRepository;
  private final ModuleJpaMapper mapper;

  public JpaModuleRepository(ModuleJpaRepository jpaRepository, ModuleJpaMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Module save(Module module) {
    return mapper.toDomain(jpaRepository.save(mapper.toJpa(module)));
  }

  @Override
  public Optional<Module> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<Module> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public void delete(Long id) {
    jpaRepository.deleteById(id);
  }

  @Override
  public int reactivate(Long id) {
    return jpaRepository.reactivate(id);
  }
}
