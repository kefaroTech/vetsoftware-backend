package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.in.GetAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class GetAppointmentService implements GetAppointmentUseCase {
    private final AppointmentRepository repository;

    public GetAppointmentService(AppointmentRepository repository) {
        this.repository = repository;
    }

    @Override
    public AppointmentDto findById(Long id, Long companyId) {
        Appointment appointment = repository.findByIdAndCompanyId(id, companyId)
            .orElseThrow(() -> new AppointmentNotFoundException(id));
        return AppointmentDto.from(appointment);
    }
}
