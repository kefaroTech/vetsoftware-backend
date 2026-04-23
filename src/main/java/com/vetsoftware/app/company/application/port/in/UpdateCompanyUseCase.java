package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.company.application.command.UpdateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;

public interface UpdateCompanyUseCase {
    @RequiresPermission("admin.all")
    CompanyDto execute(UpdateCompanyCommand command, AuthContext auth);
}
