package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.auth.application.annotation.RequiresPermission;
import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.company.application.dto.CompanyDto;
import java.util.List;

public interface ListCompaniesUseCase {
    @RequiresPermission({"admin.all","company.read"})
    List<CompanyDto> listAll(AuthContext auth);
}
