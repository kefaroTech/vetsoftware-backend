package com.vetsoftware.app.employee.application.port.out;

import com.vetsoftware.app.employee.domain.RoleSnapshot;
import java.util.List;
import java.util.Map;

public interface EmployeeRolesQueryPort {
    Map<Long, List<RoleSnapshot>> findRolesByEmployeeIds(List<Long> employeeIds);
}
