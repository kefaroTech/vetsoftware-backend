package com.vetsoftware.app.passwordreset.application.usecase;

import com.vetsoftware.app.passwordreset.application.command.RequestPasswordResetCommand;
import com.vetsoftware.app.passwordreset.application.port.in.RequestPasswordResetUseCase;
import com.vetsoftware.app.passwordreset.application.port.out.EmployeeAccountLookupPort;
import com.vetsoftware.app.passwordreset.application.port.out.EmployeeAccountLookupPort.EmployeeAccount;
import com.vetsoftware.app.passwordreset.application.port.out.PasswordResetEmailSender;
import com.vetsoftware.app.passwordreset.application.port.out.PasswordResetTokenRepository;
import com.vetsoftware.app.passwordreset.domain.PasswordResetToken;
import io.micrometer.observation.annotation.Observed;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solicitud de restablecimiento por código de empleado. Anti-enumeración: si el
 * código no existe o la cuenta no es elegible (correo sin verificar), termina
 * sin error y sin enviar nada. Si es elegible, invalida los tokens previos,
 * guarda el HASH de un token nuevo y envía el enlace por correo
 * (async/best-effort).
 */
@Observed(name = "password.reset.request")
@Service
public class RequestPasswordResetService implements RequestPasswordResetUseCase {

    private static final Logger log = LoggerFactory.getLogger(RequestPasswordResetService.class);

    private final EmployeeAccountLookupPort employeeLookup;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetEmailSender emailSender;
    private final long ttlHours;

    public RequestPasswordResetService(EmployeeAccountLookupPort employeeLookup,
            PasswordResetTokenRepository tokenRepository, PasswordResetEmailSender emailSender,
            @Value("${vetsoftware.password-reset.token-ttl-hours:1}") long ttlHours) {
        this.employeeLookup = employeeLookup;
        this.tokenRepository = tokenRepository;
        this.emailSender = emailSender;
        this.ttlHours = ttlHours;
    }

    @Override
    @Transactional
    public void execute(RequestPasswordResetCommand command) {
        String code = command.employeeCode() == null ? "" : command.employeeCode().trim();
        if (code.isEmpty())
            return;

        EmployeeAccount account = employeeLookup.findByCode(code).orElse(null);
        // Anti-enumeración: no revelamos si el código existe. Solo elegibles con correo
        // verificado
        // reciben correo.
        if (account == null || !account.emailVerified()) {
            log.info(
                    "Solicitud de restablecimiento para un código no elegible (se ignora silenciosamente)");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        tokenRepository.consumeActiveForEmployee(account.id(), account.companyId(), now);

        String rawToken = PasswordResetTokens.generateRawToken();
        tokenRepository.save(PasswordResetToken.issue(account.id(), account.companyId(),
                PasswordResetTokens.hash(rawToken), now.plusHours(ttlHours)));

        emailSender.send(account.email(), account.name(), code, account.companyName(), rawToken);
    }
}
