package com.vetsoftware.app.baserole.infrastructure.persistence;

import com.vetsoftware.app.baserole.application.port.out.BaseRoleRepository;
import com.vetsoftware.app.baserole.domain.BaseRole;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaBaseRoleRepository implements BaseRoleRepository {
  private final BaseRoleJpaRepository jpaRepository;
  private final BaseRoleJpaMapper mapper;

  public JpaBaseRoleRepository(BaseRoleJpaRepository jpaRepository, BaseRoleJpaMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public BaseRole save(BaseRole baseRole) {
    return mapper.toDomain(jpaRepository.save(mapper.toJpa(baseRole)));
  }

  @Override
  public Optional<BaseRole> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<BaseRole> findAll() {
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
