package com.vetsoftware.app.companytrialgrant.application.port.in;

import com.vetsoftware.app.companytrialgrant.application.command.ConsumeTrialGrantCommand;
import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Resuelve una prueba: escribe cuándo acabó y cómo.
 *
 * <p>
 * <strong>No borra ni desactiva la concesión.</strong> Escribe su desenlace,
 * que es lo que convierte «cuántas de las pruebas de esta campaña acabaron
 * pagando» en una consulta. Quitar un módulo antes de vencer lo marca
 * {@code ABANDONED}: la concesión sigue existiendo y ese artículo sigue sin
 * poder regalarse otra vez.
 *
 * <p>
 * Autorización: {@code hasRole('SYSTEM')} a secas. Lo dispara el barrido de
 * vencimientos o una operación comercial.
 */
public interface ConsumeTrialGrantUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    CompanyTrialGrantDto execute(ConsumeTrialGrantCommand command);
}
