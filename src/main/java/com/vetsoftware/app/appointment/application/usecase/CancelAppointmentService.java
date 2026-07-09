package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.command.CancelAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.in.CancelAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CancelAppointmentService implements CancelAppointmentUseCase {
    private final AppointmentRepository repository;

    public CancelAppointmentService(AppointmentRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public AppointmentDto execute(CancelAppointmentCommand command) {
        Appointment appointment = repository.findByIdAndCompanyId(command.id(), command.companyId())
            .orElseThrow(() -> new AppointmentNotFoundException(command.id()));
        appointment.cancel(command.reason());
        return AppointmentDto.from(repository.save(appointment));
    }
}
