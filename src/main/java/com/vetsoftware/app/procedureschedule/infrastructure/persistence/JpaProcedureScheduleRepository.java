package com.vetsoftware.app.procedureschedule.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence.HospitalizationProcedureJpaEntity;
import com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence.HospitalizationProcedureJpaRepository;
import com.vetsoftware.app.procedureschedule.application.port.out.ProcedureScheduleRepository;
import com.vetsoftware.app.procedureschedule.domain.ProcedureSchedule;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaProcedureScheduleRepository implements ProcedureScheduleRepository {
    private final ProcedureScheduleJpaRepository jpaRepository;
    private final ProcedureScheduleJpaMapper mapper;
    private final HospitalizationProcedureJpaRepository hospitalizationProcedureJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaProcedureScheduleRepository(ProcedureScheduleJpaRepository jpaRepository,
            ProcedureScheduleJpaMapper mapper,
            HospitalizationProcedureJpaRepository hospitalizationProcedureJpaRepository,
            EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.hospitalizationProcedureJpaRepository = hospitalizationProcedureJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public ProcedureSchedule save(ProcedureSchedule procedureSchedule) {
        HospitalizationProcedureJpaEntity hospitalizationProcedure = hospitalizationProcedureJpaRepository
                .getReferenceById(procedureSchedule.getHospitalizationProcedure().id());
        EmployeeJpaEntity createdBy = employeeJpaRepository
                .getReferenceById(procedureSchedule.getCreatedBy().id());
        ProcedureScheduleJpaEntity saved = jpaRepository
                .save(mapper.toJpa(procedureSchedule, hospitalizationProcedure, createdBy));
        return mapper.toDomain(saved, procedureSchedule.getHospitalizationProcedure(),
                procedureSchedule.getCreatedBy());
    }

    @Override
    public Optional<ProcedureSchedule> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ProcedureSchedule> findByHospitalizationProcedureId(
            Long hospitalizationProcedureId) {
        return jpaRepository.findByHospitalizationProcedureId(hospitalizationProcedureId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<ProcedureSchedule> findByHospitalizationId(Long hospitalizationId) {
        return jpaRepository.findByHospitalizationProcedureHospitalizationId(hospitalizationId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void disableByHospitalizationProcedureId(Long hospitalizationProcedureId) {
        jpaRepository.disableByHospitalizationProcedureId(hospitalizationProcedureId);
    }

    @Override
    public void disablePendingByHospitalizationProcedureId(Long hospitalizationProcedureId) {
        jpaRepository.disablePendingByHospitalizationProcedureId(hospitalizationProcedureId);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }
}
