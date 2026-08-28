package com.vetsoftware.app.companytrialwindow.application.usecase;

import com.vetsoftware.app.companytrialwindow.application.command.OpenTrialWindowCommand;
import com.vetsoftware.app.companytrialwindow.application.dto.CompanyTrialWindowDto;
import com.vetsoftware.app.companytrialwindow.application.port.in.OpenTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.application.port.out.CompanyTrialWindowRepository;
import com.vetsoftware.app.companytrialwindow.domain.CompanyAlreadyHasOpenTrialWindowException;
import com.vetsoftware.app.companytrialwindow.domain.CompanyTrialWindow;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abre la ventana de prueba.
 *
 * <p>
 * Comprueba antes que no haya otra abierta, aunque el índice único ya lo
 * impediría: no para sortear la restricción sino para que el operador lea qué
 * pasó. Un choque de clave a mitad de un alta comercial dice «duplicate entry
 * for key» y no dice a quién llamar.
 */
@Service
public class OpenTrialWindowService implements OpenTrialWindowUseCase {

    private final CompanyTrialWindowRepository repository;
    private final Clock clock;

    public OpenTrialWindowService(CompanyTrialWindowRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanyTrialWindowDto execute(OpenTrialWindowCommand command) {
        if (repository.existsOpenByCompanyId(command.companyId()))
            throw new CompanyAlreadyHasOpenTrialWindowException(command.companyId());
        CompanyTrialWindow window = CompanyTrialWindow.open(command.companyId(),
                command.startDate(), command.windowDays(), command.sourceQuoteId(),
                LocalDateTime.now(clock));
        return CompanyTrialWindowDto.from(repository.save(window));
    }
}
