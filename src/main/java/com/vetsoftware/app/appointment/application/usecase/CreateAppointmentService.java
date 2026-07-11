package com.vetsoftware.app.appointment.application.usecase;

import com.vetsoftware.app.appointment.application.command.CreateAppointmentCommand;
import com.vetsoftware.app.appointment.application.dto.AppointmentDto;
import com.vetsoftware.app.appointment.application.port.in.CreateAppointmentUseCase;
import com.vetsoftware.app.appointment.application.port.out.AnimalQueryPort;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.application.port.out.BranchQueryPort;
import com.vetsoftware.app.appointment.application.port.out.EmployeeQueryPort;
import com.vetsoftware.app.appointment.application.port.out.OwnerQueryPort;
import com.vetsoftware.app.appointment.domain.AnimalRef;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.BranchRef;
import com.vetsoftware.app.appointment.domain.CompanyRef;
import com.vetsoftware.app.appointment.domain.EmployeeRef;
import com.vetsoftware.app.appointment.domain.OwnerRef;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "appointment.create")
@Service
public class CreateAppointmentService implements CreateAppointmentUseCase {
    private final AppointmentRepository repository;
    private final AnimalQueryPort animalQueryPort;
    private final OwnerQueryPort ownerQueryPort;
    private final EmployeeQueryPort employeeQueryPort;
    private final BranchQueryPort branchQueryPort;

    public CreateAppointmentService(AppointmentRepository repository, AnimalQueryPort animalQueryPort,
                                    OwnerQueryPort ownerQueryPort, EmployeeQueryPort employeeQueryPort,
                                    BranchQueryPort branchQueryPort) {
        this.repository = repository;
        this.animalQueryPort = animalQueryPort;
        this.ownerQueryPort = ownerQueryPort;
        this.employeeQueryPort = employeeQueryPort;
        this.branchQueryPort = branchQueryPort;
    }

    @Override
    @Transactional
    public AppointmentDto execute(CreateAppointmentCommand command) {
        EmployeeRef employee = employeeQueryPort.findByIdAndCompanyId(command.employeeId(), command.companyId())
            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + command.employeeId()));
        AnimalRef animal = command.animalId() == null ? null
            : animalQueryPort.findByIdAndCompanyId(command.animalId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Animal not found: " + command.animalId()));
        OwnerRef owner = command.ownerId() == null ? null
            : ownerQueryPort.findByIdAndCompanyId(command.ownerId(), command.companyId())
                .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + command.ownerId()));

        // Sede: si el request trae branchId debe pertenecer a la empresa y estar ACTIVA; si no, la sede
        // activa por defecto ("Principal" activa / primera activa). No se agenda en una sede fuera de operación.
        BranchRef branch = command.branchId() != null
            ? resolveRequestedBranch(command.branchId(), command.companyId())
            : branchQueryPort.findDefaultActiveByCompanyId(command.companyId())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Company has no active branch: " + command.companyId()));

        Appointment appointment = Appointment.create(
            command.startAt(), command.type(), command.notes(), animal, owner,
            command.clientName(), command.clientPhone(), employee, CompanyRef.of(command.companyId()), branch);
        Appointment saved = repository.save(appointment);

        List<Long> clashes = repository.findClashingIds(
            command.companyId(), command.employeeId(), command.startAt(), saved.getId());
        return AppointmentDto.from(saved, clashes);
    }

    // Sede solicitada explícitamente: activa y de la empresa. Distingue "inactiva" de "inexistente" para dar
    // un error preciso (la sede existe pero fue desactivada vs. no pertenece a la empresa / no existe).
    private BranchRef resolveRequestedBranch(Long branchId, Long companyId) {
        return branchQueryPort.findActiveByIdAndCompanyId(branchId, companyId)
            .orElseThrow(() -> branchQueryPort.existsByIdAndCompanyId(branchId, companyId)
                ? new IllegalArgumentException("Branch is not active: " + branchId)
                : new IllegalArgumentException("Branch not found: " + branchId));
    }
}
