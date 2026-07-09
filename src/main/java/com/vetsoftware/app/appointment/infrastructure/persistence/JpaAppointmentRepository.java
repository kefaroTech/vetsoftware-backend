package com.vetsoftware.app.appointment.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaRepository;
import com.vetsoftware.app.appointment.application.port.out.AppointmentRepository;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAppointmentRepository implements AppointmentRepository {
    private final AppointmentJpaRepository jpaRepository;
    private final AppointmentJpaMapper mapper;
    private final AnimalJpaRepository animalJpaRepository;
    private final OwnerJpaRepository ownerJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaAppointmentRepository(AppointmentJpaRepository jpaRepository, AppointmentJpaMapper mapper,
                                    AnimalJpaRepository animalJpaRepository, OwnerJpaRepository ownerJpaRepository,
                                    EmployeeJpaRepository employeeJpaRepository,
                                    CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.animalJpaRepository = animalJpaRepository;
        this.ownerJpaRepository = ownerJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Appointment save(Appointment appointment) {
        AnimalJpaEntity animal = appointment.getAnimal() == null ? null
            : animalJpaRepository.getReferenceById(appointment.getAnimal().id());
        OwnerJpaEntity owner = appointment.getOwner() == null ? null
            : ownerJpaRepository.getReferenceById(appointment.getOwner().id());
        EmployeeJpaEntity employee = employeeJpaRepository.getReferenceById(appointment.getEmployee().id());
        CompanyJpaEntity company = companyJpaRepository.getReferenceById(appointment.getCompany().id());
        AppointmentJpaEntity saved = jpaRepository.save(
            mapper.toJpa(appointment, animal, owner, employee, company));
        return mapper.toDomain(saved, appointment.getAnimal(), appointment.getOwner(),
            appointment.getEmployee(), appointment.getCompany());
    }

    @Override
    public Optional<Appointment> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<Appointment> findByFilters(Long companyId, LocalDateTime from, LocalDateTime to,
                                           Long employeeId, AppointmentStatus status) {
        String statusName = status == null ? null : status.name();
        return jpaRepository.findByFilters(companyId, from, to, employeeId, statusName)
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Long> findClashingIds(Long companyId, Long employeeId, LocalDateTime startAt, Long excludeId) {
        return jpaRepository.findClashingIds(companyId, employeeId, startAt, excludeId);
    }

    @Override
    public void delete(Long id, Long companyId) {
        jpaRepository.softDelete(id, companyId);
    }
}
