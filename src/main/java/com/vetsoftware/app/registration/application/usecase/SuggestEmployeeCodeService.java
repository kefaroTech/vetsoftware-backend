package com.vetsoftware.app.registration.application.usecase;

import com.vetsoftware.app.registration.application.port.in.SuggestEmployeeCodeUseCase;
import com.vetsoftware.app.registration.application.port.out.EmployeeCodeChecker;
import org.springframework.stereotype.Service;

@Service
public class SuggestEmployeeCodeService implements SuggestEmployeeCodeUseCase {

    private final EmployeeCodeChecker employeeCodeChecker;

    public SuggestEmployeeCodeService(EmployeeCodeChecker employeeCodeChecker) {
        this.employeeCodeChecker = employeeCodeChecker;
    }

    @Override
    public String suggest(String companyName, String employeeName) {
        return EmployeeCodeGenerator.generateAvailable(
                companyName == null ? "" : companyName,
                employeeName == null ? "" : employeeName,
                employeeCodeChecker::exists);
    }
}
