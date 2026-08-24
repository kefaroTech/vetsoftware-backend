package com.vetsoftware.app.configurator.application.port.in;

import com.vetsoftware.app.configurator.application.command.CreateConfiguratorQuestionCommand;
import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Editar el cuestionario es {@code hasRole('SYSTEM')} a secas, sin disyunto por
 * {@code authority}: no hay ninguna empresa a la que pertenezca una pregunta, y
 * un permiso de tenant aquí le entregaría el asistente de venta de la
 * plataforma al administrador de una clínica.
 */
public interface CreateConfiguratorQuestionUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    ConfiguratorQuestionDto execute(CreateConfiguratorQuestionCommand command);
}
