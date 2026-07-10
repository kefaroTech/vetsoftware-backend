package com.vetsoftware.app.employee.application.usecase;

import com.vetsoftware.app.employee.application.port.in.SuggestEmployeeCodeUseCase;
import com.vetsoftware.app.employee.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.CompanyRef;
import org.springframework.stereotype.Service;

@Service
public class SuggestEmployeeCodeService implements SuggestEmployeeCodeUseCase {

    private final CompanyQueryPort companyQueryPort;
    private final EmployeeRepository repository;

    public SuggestEmployeeCodeService(CompanyQueryPort companyQueryPort, EmployeeRepository repository) {
        this.companyQueryPort = companyQueryPort;
        this.repository = repository;
    }

    @Override
    public String suggest(Long companyId, String name) {
        String companyName = companyQueryPort.findById(companyId).map(CompanyRef::name).orElse("");
        return EmployeeCodeGenerator.generateAvailable(
                companyName, name == null ? "" : name, repository::codeExists);
    }
}
