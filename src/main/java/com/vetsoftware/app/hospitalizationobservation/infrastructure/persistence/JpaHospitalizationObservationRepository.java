package com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaHospitalizationObservationRepository implements HospitalizationObservationRepository {
    private final HospitalizationObservationJpaRepository jpaRepository;
    private final HospitalizationObservationJpaMapper mapper;
    private final HospitalizationJpaRepository hospitalizationJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaHospitalizationObservationRepository(HospitalizationObservationJpaRepository jpaRepository,
                                                   HospitalizationObservationJpaMapper mapper,
                                                   HospitalizationJpaRepository hospitalizationJpaRepository,
                                                   EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.hospitalizationJpaRepository = hospitalizationJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public HospitalizationObservation save(HospitalizationObservation observation) {
        HospitalizationJpaEntity hospitalization =
            hospitalizationJpaRepository.getReferenceById(observation.getHospitalization().id());
        EmployeeJpaEntity createdBy =
            employeeJpaRepository.getReferenceById(observation.getCreatedBy().id());
        HospitalizationObservationJpaEntity saved =
            jpaRepository.save(mapper.toJpa(observation, hospitalization, createdBy));
        return mapper.toDomain(saved, observation.getHospitalization(), observation.getCreatedBy());
    }

    @Override
    public Optional<HospitalizationObservation> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<HospitalizationObservation> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndHospitalization_Company_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<HospitalizationObservation> findAllByHospitalizationId(Long hospitalizationId) {
        return jpaRepository.findByHospitalizationId(hospitalizationId).stream()
            .map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }
}
