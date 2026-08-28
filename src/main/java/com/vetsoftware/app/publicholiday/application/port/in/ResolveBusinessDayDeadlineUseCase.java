package com.vetsoftware.app.publicholiday.application.port.in;

import com.vetsoftware.app.publicholiday.application.command.ResolveBusinessDayDeadlineCommand;
import com.vetsoftware.app.publicholiday.application.dto.BusinessDayDeadlineDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * <strong>La operacion por la que existe este bloque.</strong> Traduce «N dias
 * habiles desde X» a una fecha, contra el calendario real.
 *
 * <p>
 * Lo van a consumir varias funcionalidades —el reclamo de datos personales (15
 * dias habiles), la consulta (10), el certificado de retencion (ultimo dia
 * habil de marzo), la ventana de retracto—, y todas comparten el mismo modo de
 * fallo: contando dias corridos el vencimiento sale <em>mas tarde</em> que el
 * real, es decir siempre en la direccion de incumplir. Que sea un caso de uso
 * propio y no un helper copiado en cada feature es lo que garantiza que la
 * correccion de un festivo llegue a todas a la vez.
 */
public interface ResolveBusinessDayDeadlineUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('holiday.read') "
            + "and @authz.isMyCompany(#command.companyId))")
    BusinessDayDeadlineDto resolve(ResolveBusinessDayDeadlineCommand command);
}
