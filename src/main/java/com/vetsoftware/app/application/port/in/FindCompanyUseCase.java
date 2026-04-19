package com.vetsoftware.app.application.port.in;

import com.vetsoftware.app.application.dto.CompanyDto;

public interface FindCompanyUseCase {
    CompanyDto findById(Long id);
}
