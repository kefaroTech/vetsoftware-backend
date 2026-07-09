package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.command.ChangeAppointmentStatusCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.in.ChangeAppointmentStatusUseCase;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeAppointmentStatusService implements ChangeAppointmentStatusUseCase {
    private final AppointmentRepository repository;

    public ChangeAppointmentStatusService(AppointmentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AppointmentDto execute(ChangeAppointmentStatusCommand command) {
        Appointment appointment = repository.findByIdAndCompanyId(command.id(), command.companyId())
            .orElseThrow(() -> new AppointmentNotFoundException(command.id()));
        appointment.transitionTo(command.status());
        return AppointmentDto.from(repository.save(appointment));
    }
}
