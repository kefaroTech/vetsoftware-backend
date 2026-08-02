package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListEmployeesByCompanyUseCase {
  @PreAuthorize(
      "hasRole('SYSTEM') or (hasAuthority('employee.read') and @authz.isMyCompany(#companyId))")
  List<EmployeeDto> listByCompany(Long companyId);
}
