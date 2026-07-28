package com.vetsoftware.app.coderecovery.application.usecase;

import com.vetsoftware.app.coderecovery.application.command.RecoverEmployeeCodeCommand;
import com.vetsoftware.app.coderecovery.application.port.in.RecoverEmployeeCodeUseCase;
import com.vetsoftware.app.coderecovery.application.port.out.CodeRecoveryEmailSender;
import com.vetsoftware.app.coderecovery.application.port.out.EmployeeAccountsByEmailPort;
import com.vetsoftware.app.coderecovery.application.port.out.EmployeeAccountsByEmailPort.EmployeeAccount;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Recordar mi código": busca todas las cuentas activas y verificadas con ese correo y le envía sus códigos
 * de acceso (una fila por veterinaria). Anti-enumeración: si no hay coincidencias, termina sin error y sin
 * enviar nada. El código no es un secreto (es el usuario), pero solo se envía al correo registrado.
 */
@Observed(name = "codeRecovery.recoverEmployeeCode")
@Service
public class RecoverEmployeeCodeService implements RecoverEmployeeCodeUseCase {

    private static final Logger log = LoggerFactory.getLogger(RecoverEmployeeCodeService.class);

    private final EmployeeAccountsByEmailPort accountsByEmail;
    private final CodeRecoveryEmailSender emailSender;

    public RecoverEmployeeCodeService(EmployeeAccountsByEmailPort accountsByEmail,
                                      CodeRecoveryEmailSender emailSender) {
        this.accountsByEmail = accountsByEmail;
        this.emailSender = emailSender;
    }

    @Override
    @Transactional(readOnly = true)
    public void execute(RecoverEmployeeCodeCommand command) {
        String email = command.email() == null ? "" : command.email().trim();
        if (email.isEmpty()) return;

        List<EmployeeAccount> accounts = accountsByEmail.findByEmail(email);
        if (accounts.isEmpty()) {
            log.info("Solicitud de recuperación de código para un correo sin cuentas elegibles (se ignora)");
            return;
        }
        emailSender.send(email, accounts.get(0).name(), accounts);
    }
}
