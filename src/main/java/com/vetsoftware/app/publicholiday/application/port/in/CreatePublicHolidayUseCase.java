package com.vetsoftware.app.publicholiday.application.port.in;

import com.vetsoftware.app.publicholiday.application.command.CreatePublicHolidayCommand;
import com.vetsoftware.app.publicholiday.application.dto.PublicHolidayDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Publica un festivo. Escribe la plataforma y solo la plataforma: un tenant que
 * pudiera anadir un festivo se estaria concediendo un dia mas de plazo en todos
 * los vencimientos del sistema.
 */
public interface CreatePublicHolidayUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PublicHolidayDto execute(CreatePublicHolidayCommand command);
}
