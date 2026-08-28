package com.vetsoftware.app.securityincident.infrastructure.orchestration;

import com.vetsoftware.app.publicholiday.application.command.ResolveBusinessDayDeadlineCommand;
import com.vetsoftware.app.publicholiday.application.port.in.ResolveBusinessDayDeadlineUseCase;
import com.vetsoftware.app.securityincident.application.port.out.BusinessDayDeadlinePort;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Resuelve el plazo en dias habiles delegando en el bloque de festivos.
 *
 * <p>
 * <strong>Este es el unico archivo de la rodaja que conoce a
 * {@code publicholiday}</strong>, y por eso vive en
 * {@code infrastructure/orchestration}: el vertical slicing prohibe que el
 * dominio o la aplicacion importen otra feature, y permite el cruce desde
 * infraestructura. Mismo patron que
 * {@code entitlement/infrastructure/orchestration/LimitDenialAdapter}.
 *
 * <p>
 * <strong>El calendario no se copia.</strong> La aritmetica de dias habiles
 * —que el dia de partida no cuenta, que sabado, domingo y festivo observado no
 * suman— vive una sola vez, en {@code HolidayCalendar}. Reimplementarla aqui
 * significaria que el dia que alguien corrija un festivo, unos plazos del
 * producto se enteren y otros no.
 *
 * <h2>La excepcion del hueco NO se captura, y es la decision del archivo</h2>
 *
 * <p>
 * Si el tramo de calendario cargado no cubre el recorrido —o entra en un año
 * sin festivos sembrados— el calculo lanza {@code HolidayCalendarGapException},
 * y aqui se <strong>deja propagar</strong>. {@code GlobalExceptionHandler} ya
 * la mapea a 409.
 *
 * <p>
 * Tragarla y caer a dias corridos seria exactamente el fallo que
 * {@code HolidayCalendar} existe para impedir: sin los festivos, el vencimiento
 * sale <em>mas tarde</em> del real, el incidente parece dentro de plazo cuando
 * ya lo incumplio, y no hay ningun error que lo delate. El error siempre cae
 * del lado de incumplir. Un incidente sin plazo calculable tiene que ser
 * ruidoso: el arreglo es sembrar los festivos del año, no aproximar.
 *
 * <p>
 * <strong>Por que {@code companyId} va a {@code null}.</strong> El gate de
 * {@code ResolveBusinessDayDeadlineUseCase} es
 * {@code hasRole('SYSTEM') or (hasAuthority('holiday.read') and @authz.isMyCompany(#command.companyId))}.
 * Los siete puertos de esta rodaja estan cerrados a {@code ROLE_SYSTEM} a
 * secas, asi que cuando se llega aqui el principal ya es SYSTEM y la primera
 * rama basta. Poner una empresa inventada seria peor que no ponerla: haria
 * pasar por acotada una llamada que no lo esta.
 */
@Component
public class BusinessDayDeadlineAdapter implements BusinessDayDeadlinePort {

    private final ResolveBusinessDayDeadlineUseCase resolveBusinessDayDeadline;

    public BusinessDayDeadlineAdapter(
            ResolveBusinessDayDeadlineUseCase resolveBusinessDayDeadline) {
        this.resolveBusinessDayDeadline = resolveBusinessDayDeadline;
    }

    @Override
    public LocalDate resolve(LocalDate start, int businessDays) {
        return resolveBusinessDayDeadline
                .resolve(new ResolveBusinessDayDeadlineCommand(start, businessDays, null))
                .dueDate();
    }
}
