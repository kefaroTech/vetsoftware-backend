package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.in.ListAppointmentsUseCase;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.query.ListAppointmentsQuery;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "appointment.list")
@Service
public class ListAppointmentsService implements ListAppointmentsUseCase {
    private final AppointmentRepository repository;
    // Inyectado y no `LocalDate.now()`: la ventana por defecto se calcula desde
    // hoy, y un test que compare contra el reloj del sistema se cae solo el dia
    // que la medianoche caiga entre dos lineas. Ver "Determinismo" del CLAUDE.md.
    private final Clock clock;

    public ListAppointmentsService(AppointmentRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Ventana por defecto cuando el cliente no manda rango, y tope de la que pida.
     * BE-06: una agenda no se pagina —una semana se pinta entera o no se pinta—
     * pero eso no es excusa para que el rango sea libre. Sin esto,
     * {@code GET /appointments} sin parametros devuelve la agenda COMPLETA de la
     * empresa, y con parametros admite {@code from=1900-01-01}, que es lo mismo por
     * otra puerta.
     *
     * <p>
     * Un anio cubre de sobra lo que cualquier vista de calendario muestra: la mas
     * amplia es la mensual.
     */
    private static final int DEFAULT_WINDOW_DAYS = 31;
    private static final int MAX_WINDOW_DAYS = 366;

    @Override
    public List<AppointmentDto> execute(ListAppointmentsQuery query) {
        LocalDate fromDate = query.from();
        LocalDate toDate = query.to();

        if (fromDate == null && toDate == null) {
            fromDate = LocalDate.now(clock);
            toDate = fromDate.plusDays(DEFAULT_WINDOW_DAYS);
        } else if (fromDate == null) {
            // «Hasta X» conserva su intencion mirando hacia atras desde X; anclarlo
            // en hoy devolveria vacio siempre que X fuese una fecha pasada.
            fromDate = toDate.minusDays(DEFAULT_WINDOW_DAYS);
        } else if (toDate == null) {
            toDate = fromDate.plusDays(DEFAULT_WINDOW_DAYS);
        }

        // Un rango invertido llega por un filtro mal armado, no por intencion: se
        // colapsa al dia de inicio en vez de dejar que la consulta se vuelva libre.
        if (toDate.isBefore(fromDate)) {
            toDate = fromDate;
        }
        if (fromDate.plusDays(MAX_WINDOW_DAYS).isBefore(toDate)) {
            toDate = fromDate.plusDays(MAX_WINDOW_DAYS);
        }

        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.atTime(LocalTime.MAX);
        return repository.findByFilters(query.companyId(), from, to, query.employeeId(),
                query.status(), query.branchId()).stream().map(AppointmentDto::from).toList();
    }
}
