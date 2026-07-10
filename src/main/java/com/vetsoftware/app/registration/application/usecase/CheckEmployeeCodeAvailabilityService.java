package com.vetsoftware.app.registration.application.usecase;

import com.vetsoftware.app.registration.application.port.in.CheckEmployeeCodeAvailabilityUseCase;
import com.vetsoftware.app.registration.application.port.out.EmployeeCodeChecker;
import org.springframework.stereotype.Service;

@Service
public class CheckEmployeeCodeAvailabilityService implements CheckEmployeeCodeAvailabilityUseCase {

    private static final int MIN_LENGTH = 4;

    private final EmployeeCodeChecker employeeCodeChecker;

    public CheckEmployeeCodeAvailabilityService(EmployeeCodeChecker employeeCodeChecker) {
        this.employeeCodeChecker = employeeCodeChecker;
    }

    @Override
    public boolean isAvailable(String employeeCode) {
        if (employeeCode == null) return false;
        String code = employeeCode.trim();
        // El formato completo lo valida el bean del request al enviar; aquí solo un piso mínimo para no
        // reportar "disponible" ante entradas triviales.
        if (code.length() < MIN_LENGTH) return false;
        return !employeeCodeChecker.exists(code);
    }
}
