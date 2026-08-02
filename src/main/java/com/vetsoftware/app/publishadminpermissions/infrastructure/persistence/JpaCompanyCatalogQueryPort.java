package com.vetsoftware.app.publishadminpermissions.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.publishadminpermissions.application.port.out.CompanyAdminContext;
import com.vetsoftware.app.publishadminpermissions.application.port.out.CompanyCatalogQueryPort;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaEntity;
import com.vetsoftware.app.role.infrastructure.persistence.RoleJpaRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaCompanyCatalogQueryPort implements CompanyCatalogQueryPort {

  private static final String ADMIN_CODE = "ADMIN";

  private final RoleJpaRepository roleJpaRepository;

  public JpaCompanyCatalogQueryPort(RoleJpaRepository roleJpaRepository) {
    this.roleJpaRepository = roleJpaRepository;
  }

  @Override
  public List<CompanyAdminContext> findAllWithAdminRole() {
    return roleJpaRepository.findAllByCode(ADMIN_CODE).stream().map(this::toContext).toList();
  }

  private CompanyAdminContext toContext(RoleJpaEntity role) {
    CompanyJpaEntity company = role.getCompany();
    return new CompanyAdminContext(company.getId(), company.getMembership().getId(), role.getId());
  }
}
