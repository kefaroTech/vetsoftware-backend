package com.vetsoftware.app.coderecovery.infrastructure.web;

import com.vetsoftware.app.coderecovery.application.command.RecoverEmployeeCodeCommand;
import com.vetsoftware.app.coderecovery.application.port.in.RecoverEmployeeCodeUseCase;
import com.vetsoftware.app.coderecovery.infrastructure.web.request.RecoverCodeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class CodeRecoveryController {

    private final RecoverEmployeeCodeUseCase recoverUseCase;

    public CodeRecoveryController(RecoverEmployeeCodeUseCase recoverUseCase) {
        this.recoverUseCase = recoverUseCase;
    }

    /** "Recordar mi código" por correo. Responde 204 SIEMPRE (anti-enumeración: no revela si el correo existe). */
    @PostMapping("/recover-code")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void recoverCode(@Valid @RequestBody RecoverCodeRequest request) {
        recoverUseCase.execute(new RecoverEmployeeCodeCommand(request.email()));
    }
}
