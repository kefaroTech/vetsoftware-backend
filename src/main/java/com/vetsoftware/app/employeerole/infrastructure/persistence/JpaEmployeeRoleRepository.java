package com.vetsoftware.app.employeerole.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.employeerole.application.port.out.EmployeeRoleRepository;
import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEmployeeRoleRepository implements EmployeeRoleRepository {
  private final EmployeeRoleJpaRepository jpaRepository;
  private final EmployeeRoleJpaMapper mapper;
  private final EmployeeJpaRepository employeeJpaRepository;
  private final RoleJpaRepository roleJpaRepository;

  public JpaEmployeeRoleRepository(
      EmployeeRoleJpaRepository jpaRepository,
      EmployeeRoleJpaMapper mapper,
      EmployeeJpaRepository employeeJpaRepository,
      RoleJpaRepository roleJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.employeeJpaRepository = employeeJpaRepository;
    this.roleJpaRepository = roleJpaRepository;
  }

  @Override
  public EmployeeRole save(EmployeeRole employeeRole) {
    EmployeeJpaEntity employee =
        employeeJpaRepository.getReferenceById(employeeRole.getEmployee().id());
    RoleJpaEntity role = roleJpaRepository.getReferenceById(employeeRole.getRole().id());
    EmployeeRoleJpaEntity saved = jpaRepository.save(mapper.toJpa(employeeRole, employee, role));
    return mapper.toDomain(saved, employeeRole.getEmployee(), employeeRole.getRole());
  }

  @Override
  public Optional<EmployeeRole> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<EmployeeRole> findAll() {
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

  @Override
  public Optional<Long> findDisabledIdByEmployeeAndRole(Long employeeId, Long roleId) {
    return jpaRepository.findDisabledIdByEmployeeAndRole(employeeId, roleId);
  }
}
