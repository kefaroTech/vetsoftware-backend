package com.vetsoftware.app.registration.infrastructure.persistence;

import com.vetsoftware.app.baserole.infrastructure.persistence.BaseRoleJpaRepository;
import com.vetsoftware.app.registration.application.port.out.BaseRoleProvider;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaBaseRoleProvider implements BaseRoleProvider {

  private final BaseRoleJpaRepository baseRoleJpaRepository;

  public JpaBaseRoleProvider(BaseRoleJpaRepository baseRoleJpaRepository) {
    this.baseRoleJpaRepository = baseRoleJpaRepository;
  }

  @Override
  public List<BaseRoleData> findAll() {
    // @SQLRestriction("enabled = true") en la entidad ya filtra los deshabilitados.
    return baseRoleJpaRepository.findAll().stream()
        .map(
            e ->
                new BaseRoleData(
                    e.getId(), e.getName(), e.getCode(), Boolean.TRUE.equals(e.getMandatory())))
        .toList();
  }
}
