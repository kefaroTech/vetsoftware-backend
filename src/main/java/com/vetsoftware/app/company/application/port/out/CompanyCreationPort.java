package com.vetsoftware.app.company.application.port.out;

import com.vetsoftware.app.company.application.command.CreateCompanyCommand;
import com.vetsoftware.app.company.application.dto.CompanyDto;

/** Acceso local a la primitiva de creación que también usa registration. */
public interface CompanyCreationPort {

    CompanyDto create(CreateCompanyCommand command);
}
