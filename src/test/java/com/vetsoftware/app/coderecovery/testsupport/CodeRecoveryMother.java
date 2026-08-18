package com.vetsoftware.app.coderecovery.testsupport;

import com.vetsoftware.app.coderecovery.application.port.out.EmployeeAccountsByEmailPort.EmployeeAccount;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import java.util.List;

/**
 * Fixtures de la feature coderecovery. {@link #empleadoJpa} y
 * {@link #companyJpa} construyen entidades JPA de OTRAS features (constructor
 * protegido, sin builder público) por reflexión — mismo patrón que
 * {@code auth.testsupport.ReflectionEntities} — porque
 * {@link JpaEmployeeAccountsByEmailPort} las consulta vía el cruce permitido de
 * vertical slicing.
 */
public final class CodeRecoveryMother {

    private CodeRecoveryMother() {
    }

    public static EmployeeAccount cuentaVeterinariaCentral() {
        return new EmployeeAccount("Juan Pérez", "EMP001", "Veterinaria Central");
    }

    public static EmployeeAccount cuentaVeterinariaNorte() {
        return new EmployeeAccount("Juan Pérez", "EMP002", "Veterinaria Norte");
    }

    public static List<EmployeeAccount> dosCuentas() {
        return List.of(cuentaVeterinariaCentral(), cuentaVeterinariaNorte());
    }

    public static EmployeeJpaEntity empleadoJpa(String name, String employeeCode,
            CompanyJpaEntity company) throws ReflectiveOperationException {
        EmployeeJpaEntity entity = newInstance(EmployeeJpaEntity.class);
        entity.setName(name);
        entity.setEmployeeCode(employeeCode);
        entity.setCompany(company);
        return entity;
    }

    public static CompanyJpaEntity companyJpa(String name) throws ReflectiveOperationException {
        CompanyJpaEntity company = newInstance(CompanyJpaEntity.class);
        company.setName(name);
        return company;
    }

    private static <T> T newInstance(Class<T> type) throws ReflectiveOperationException {
        var constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }
}
