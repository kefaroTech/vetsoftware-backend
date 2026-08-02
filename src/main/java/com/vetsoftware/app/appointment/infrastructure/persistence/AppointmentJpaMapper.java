package com.vetsoftware.app.appointment.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.appointment.domain.AnimalRef;
import com.vetsoftware.app.appointment.domain.Appointment;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.appointment.domain.AppointmentType;
import com.vetsoftware.app.appointment.domain.BranchRef;
import com.vetsoftware.app.appointment.domain.CompanyRef;
import com.vetsoftware.app.appointment.domain.EmployeeRef;
import com.vetsoftware.app.appointment.domain.OwnerRef;
import com.vetsoftware.app.branch.infrastructure.persistence.BranchJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AppointmentJpaMapper {

    public AppointmentJpaEntity toJpa(Appointment appointment, AnimalJpaEntity animal,
            OwnerJpaEntity owner, EmployeeJpaEntity employee, CompanyJpaEntity company,
            BranchJpaEntity branch) {
        AppointmentJpaEntity entity = new AppointmentJpaEntity();
        entity.setId(appointment.getId());
        entity.setStartAt(appointment.getStartAt());
        entity.setType(appointment.getType().name());
        entity.setStatus(appointment.getStatus().name());
        entity.setNotes(appointment.getNotes());
        entity.setCancellationReason(appointment.getCancellationReason());
        entity.setAnimal(animal);
        entity.setOwner(owner);
        entity.setClientName(appointment.getClientName());
        entity.setClientPhone(appointment.getClientPhone());
        entity.setClientEmail(appointment.getClientEmail());
        entity.setEmployee(employee);
        entity.setCompany(company);
        entity.setBranch(branch);
        entity.setVersion(appointment.getVersion());
        entity.setEnabled(appointment.isEnabled());
        entity.setCreatedDate(appointment.getCreatedDate());
        return entity;
    }

    // Read path — el @EntityGraph ya hidrató animal/owner/employee/company/branch.
    public Appointment toDomain(AppointmentJpaEntity entity) {
        AnimalJpaEntity a = entity.getAnimal();
        OwnerJpaEntity o = entity.getOwner();
        EmployeeJpaEntity e = entity.getEmployee();
        BranchJpaEntity b = entity.getBranch();
        AnimalRef animalRef = a == null ? null : new AnimalRef(a.getId(), a.getName(), a.getCode());
        OwnerRef ownerRef = o == null ? null : new OwnerRef(o.getId(), o.getName());
        EmployeeRef employeeRef = new EmployeeRef(e.getId(), e.getName());
        CompanyRef companyRef = new CompanyRef(entity.getCompany().getId());
        BranchRef branchRef = new BranchRef(b.getId(), b.getName(), b.getCode());
        return toDomain(entity, animalRef, ownerRef, employeeRef, companyRef, branchRef);
    }

    // Write path — reusa los refs ya cargados para no disparar los proxies.
    public Appointment toDomain(AppointmentJpaEntity entity, AnimalRef animalRef, OwnerRef ownerRef,
            EmployeeRef employeeRef, CompanyRef companyRef, BranchRef branchRef) {
        return new Appointment(entity.getId(), entity.getStartAt(),
                AppointmentType.valueOf(entity.getType()),
                AppointmentStatus.valueOf(entity.getStatus()), entity.getNotes(),
                entity.getCancellationReason(), animalRef, ownerRef, entity.getClientName(),
                entity.getClientPhone(), entity.getClientEmail(), employeeRef, companyRef,
                branchRef, entity.getVersion(), entity.isEnabled(), entity.getCreatedDate());
    }
}
