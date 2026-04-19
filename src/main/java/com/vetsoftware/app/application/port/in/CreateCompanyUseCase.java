package com.vetsoftware.app.application.port.in;

import com.vetsoftware.app.application.command.CreateCompanyCommand;
import com.vetsoftware.app.application.dto.CompanyDto;

public interface CreateCompanyUseCase {
    CompanyDto execute(CreateCompanyCommand command);
}
