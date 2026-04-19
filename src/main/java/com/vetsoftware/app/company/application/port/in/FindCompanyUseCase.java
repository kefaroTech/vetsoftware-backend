package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.dto.CompanyDto;

public interface FindCompanyUseCase {
    CompanyDto findById(Long id);
}
