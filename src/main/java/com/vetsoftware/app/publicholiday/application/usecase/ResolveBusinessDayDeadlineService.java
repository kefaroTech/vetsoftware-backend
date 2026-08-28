package com.vetsoftware.app.publicholiday.application.usecase;

import com.vetsoftware.app.publicholiday.application.command.ResolveBusinessDayDeadlineCommand;
import com.vetsoftware.app.publicholiday.application.dto.BusinessDayDeadlineDto;
import com.vetsoftware.app.publicholiday.application.port.in.ResolveBusinessDayDeadlineUseCase;
import com.vetsoftware.app.publicholiday.application.port.out.PublicHolidayRepository;
import com.vetsoftware.app.publicholiday.domain.HolidayCalendar;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

/**
 * Traduce «N dias habiles desde X» a una fecha.
 *
 * <p>
 * El servicio hace tres cosas y ninguna es el calculo: resuelve el «hoy» con el
 * reloj inyectado, decide <em>cuanto</em> calendario hay que cargar y se lo
 * pide al puerto. La aritmetica vive en {@link HolidayCalendar}, en el dominio,
 * para que se pueda probar sin mocks y sin base de datos.
 */
@Observed(name = "publicholiday.deadline")
@Service
public class ResolveBusinessDayDeadlineService implements ResolveBusinessDayDeadlineUseCase {

    /**
     * Margen del tramo que se carga, en dias naturales por dia habil pedido.
     *
     * <p>
     * Tres es una cota holgada y deliberada: siete dias naturales bastan para cinco
     * habiles, o sea 1,4 por habil, y ni una semana entera de festivos seguidos
     * llega a tres. Cargar de mas cuesta unas filas; cargar de menos cuesta un
     * {@code HolidayCalendarGapException} en un plazo perfectamente calculable.
     */
    private static final int DIAS_NATURALES_POR_HABIL = 3;

    /** Colchon fijo por si el plazo arranca en mitad de un puente largo. */
    private static final int COLCHON_EN_DIAS = 30;

    private final PublicHolidayRepository repository;
    private final Clock clock;

    public ResolveBusinessDayDeadlineService(PublicHolidayRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public BusinessDayDeadlineDto resolve(ResolveBusinessDayDeadlineCommand command) {
        LocalDate start = command.startDate() == null ? LocalDate.now(clock) : command.startDate();
        LocalDate hasta = start.plusDays(
                (long) command.businessDays() * DIAS_NATURALES_POR_HABIL + COLCHON_EN_DIAS);
        HolidayCalendar calendar = repository.loadCalendar(start, hasta);
        LocalDate dueDate = calendar.deadline(start, command.businessDays());
        return new BusinessDayDeadlineDto(start, command.businessDays(), dueDate,
                calendar.weekdayHolidaysBetween(start, dueDate));
    }
}
