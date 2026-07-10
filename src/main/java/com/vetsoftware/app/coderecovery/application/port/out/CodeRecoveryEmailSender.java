package com.vetsoftware.app.coderecovery.application.port.out;

import com.vetsoftware.app.coderecovery.application.port.out.EmployeeAccountsByEmailPort.EmployeeAccount;
import java.util.List;

/**
 * Envía el correo con la lista de códigos de acceso (plantilla de Resend). La implementación arma el bloque
 * dinámico {@code ACCOUNTS_HTML} con una fila por cuenta. Async/best-effort (no bloquea).
 */
public interface CodeRecoveryEmailSender {
    void send(String toEmail, String employeeName, List<EmployeeAccount> accounts);
}
