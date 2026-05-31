package com.vetsoftware.app.medicationschedule.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence.HospitalizationMedicationJpaEntity;
import com.vetsoftware.app.hospitalizationmedication.infrastructure.persistence.HospitalizationMedicationJpaRepository;
import com.vetsoftware.app.medicationschedule.application.port.out.MedicationScheduleRepository;
import com.vetsoftware.app.medicationschedule.domain.MedicationSchedule;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaMedicationScheduleRepository implements MedicationScheduleRepository {
    private final MedicationScheduleJpaRepository jpaRepository;
    private final MedicationScheduleJpaMapper mapper;
    private final HospitalizationMedicationJpaRepository hospitalizationMedicationJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaMedicationScheduleRepository(MedicationScheduleJpaRepository jpaRepository,
                                           MedicationScheduleJpaMapper mapper,
                                           HospitalizationMedicationJpaRepository hospitalizationMedicationJpaRepository,
                                           EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.hospitalizationMedicationJpaRepository = hospitalizationMedicationJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public MedicationSchedule save(MedicationSchedule medicationSchedule) {
        HospitalizationMedicationJpaEntity hospitalizationMedication =
            hospitalizationMedicationJpaRepository.getReferenceById(medicationSchedule.getHospitalizationMedication().id());
        EmployeeJpaEntity createdBy =
            employeeJpaRepository.getReferenceById(medicationSchedule.getCreatedBy().id());
        MedicationScheduleJpaEntity saved =
            jpaRepository.save(mapper.toJpa(medicationSchedule, hospitalizationMedication, createdBy));
        return mapper.toDomain(saved, medicationSchedule.getHospitalizationMedication(), medicationSchedule.getCreatedBy());
    }

    @Override
    public Optional<MedicationSchedule> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<MedicationSchedule> findByHospitalizationMedicationId(Long hospitalizationMedicationId) {
        return jpaRepository.findByHospitalizationMedicationId(hospitalizationMedicationId).stream()
            .map(mapper::toDomain).toList();
    }

    @Override
    public List<MedicationSchedule> findByHospitalizationId(Long hospitalizationId) {
        return jpaRepository.findByHospitalizationMedicationHospitalizationId(hospitalizationId).stream()
            .map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public void disableByHospitalizationMedicationId(Long hospitalizationMedicationId) {
        jpaRepository.disableByHospitalizationMedicationId(hospitalizationMedicationId);
    }

    @Override
    public void disablePendingByHospitalizationMedicationId(Long hospitalizationMedicationId) {
        jpaRepository.disablePendingByHospitalizationMedicationId(hospitalizationMedicationId);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }
}
