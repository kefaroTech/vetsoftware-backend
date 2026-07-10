package com.vetsoftware.app.coderecovery.application.port.out;

import java.util.List;

/**
 * Busca todas las cuentas de empleado (de cualquier veterinaria) asociadas a un correo, para recordarle sus
 * códigos de acceso. Solo devuelve cuentas activas (habilitadas) y con el correo verificado — una sin
 * verificar no puede iniciar sesión igual.
 */
public interface EmployeeAccountsByEmailPort {
    List<EmployeeAccount> findByEmail(String email);

    record EmployeeAccount(String name, String code, String companyName) {}
}
