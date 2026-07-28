package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.in.ListAppointmentsUseCase;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.query.ListAppointmentsQuery;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "appointment.list")
@Service
public class ListAppointmentsService implements ListAppointmentsUseCase {
    private final AppointmentRepository repository;

    public ListAppointmentsService(AppointmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AppointmentDto> execute(ListAppointmentsQuery query) {
        LocalDateTime from = query.from() == null ? null : query.from().atStartOfDay();
        LocalDateTime to = query.to() == null ? null : query.to().atTime(LocalTime.MAX);
        return repository.findByFilters(query.companyId(), from, to, query.employeeId(), query.status(),
                query.branchId())
            .stream().map(AppointmentDto::from).toList();
    }
}
