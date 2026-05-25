package com.vetsoftware.app.employeerole.application.port.out;

import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import java.util.List;
import java.util.Optional;

public interface EmployeeRoleRepository {
    EmployeeRole save(EmployeeRole employeeRole);
    Optional<EmployeeRole> findById(Long id);
    List<EmployeeRole> findAll();
    void delete(Long id);
    int reactivate(Long id);
}
