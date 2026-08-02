package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.port.in.DeleteAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.domain.AppointmentNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "appointment.delete")
@Service
public class DeleteAppointmentService implements DeleteAppointmentUseCase {
  private final AppointmentRepository repository;

  public DeleteAppointmentService(AppointmentRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void execute(Long id, Long companyId) {
    repository
        .findByIdAndCompanyId(id, companyId)
        .orElseThrow(() -> new AppointmentNotFoundException(id));
    repository.delete(id, companyId);
  }
}
