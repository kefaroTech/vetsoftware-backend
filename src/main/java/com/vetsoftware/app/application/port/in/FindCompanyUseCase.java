package com.vetsoftware.app.application.port.in;

import com.vetsoftware.app.application.dto.CompanyDto;
import com.vetsoftware.app.domain.CompanyId;

public interface FindCompanyUseCase {
    CompanyDto findById(CompanyId id);
}
