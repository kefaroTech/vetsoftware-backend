package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.command.ChangeAppointmentStatusCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.in.ChangeAppointmentStatusUseCase;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics.Channel;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "appointment.change.status")
@Service
public class ChangeAppointmentStatusService implements ChangeAppointmentStatusUseCase {
  private final AppointmentRepository repository;
  private final AppointmentMetrics appointmentMetrics;

  public ChangeAppointmentStatusService(
      AppointmentRepository repository, AppointmentMetrics appointmentMetrics) {
    this.repository = repository;
    this.appointmentMetrics = appointmentMetrics;
  }

  @Override
  @Transactional
  public AppointmentDto execute(ChangeAppointmentStatusCommand command) {
    Appointment appointment =
        repository
            .findByIdAndCompanyId(command.id(), command.companyId())
            .orElseThrow(() -> new AppointmentNotFoundException(command.id()));
    appointment.transitionTo(command.status());
    Appointment saved = repository.save(appointment);
    appointmentMetrics.transitioned(saved.getStatus(), Channel.STAFF);
    return AppointmentDto.from(saved);
  }
}
