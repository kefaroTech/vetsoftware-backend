package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;

public interface CreateCompanyUseCase {
    @RequiresPermission({"admin.all","company.create"})
    CompanyDto execute(CreateCompanyCommand command, AuthContext auth);
}
