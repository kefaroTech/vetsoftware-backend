package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.command.RescheduleAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.in.RescheduleAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import com.vetsoftware.app.appointment.domain.EmployeeRef;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "appointment.reschedule")
@Service
public class RescheduleAppointmentService implements RescheduleAppointmentUseCase {
  private final AppointmentRepository repository;
  private final EmployeeQueryPort employeeQueryPort;

  public RescheduleAppointmentService(
      AppointmentRepository repository, EmployeeQueryPort employeeQueryPort) {
    this.repository = repository;
    this.employeeQueryPort = employeeQueryPort;
  }

  @Override
  @Transactional
  public AppointmentDto execute(RescheduleAppointmentCommand command) {
    Appointment appointment =
        repository
            .findByIdAndCompanyId(command.id(), command.companyId())
            .orElseThrow(() -> new AppointmentNotFoundException(command.id()));
    EmployeeRef employee =
        employeeQueryPort
            .findByIdAndCompanyId(command.employeeId(), command.companyId())
            .orElseThrow(
                () -> new IllegalArgumentException("Employee not found: " + command.employeeId()));

    appointment.reschedule(command.startAt(), employee);
    Appointment saved = repository.save(appointment);

    List<Long> clashes =
        repository.findClashingIds(
            command.companyId(), command.employeeId(), command.startAt(), saved.getId());
    return AppointmentDto.from(saved, clashes);
  }
}
