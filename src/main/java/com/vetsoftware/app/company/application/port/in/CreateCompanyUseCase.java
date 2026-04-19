package com.vetsoftware.app.company.application.port.in;

import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;

public interface CreateCompanyUseCase {
    CompanyDto execute(CreateCompanyCommand command);
}
