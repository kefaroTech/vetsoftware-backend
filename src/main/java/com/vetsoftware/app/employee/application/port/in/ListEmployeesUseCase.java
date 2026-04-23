package com.vetsoftware.app.employee.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.employee.application.dto.EmployeeDto;
import java.util.List;

public interface ListEmployeesUseCase {
    @RequiresPermission({"admin.all","employee.read"})
    List<EmployeeDto> listAll(AuthContext auth);
}
