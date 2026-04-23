package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.company.application.dto.CompanyDto;

public interface FindCompanyUseCase {
    @RequiresPermission("admin.all")
    CompanyDto findById(Long id, AuthContext auth);
}
