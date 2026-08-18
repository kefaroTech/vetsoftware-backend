package com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import org.springframework.data.domain.Sort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaHospitalizationObservationRepository
        implements
            HospitalizationObservationRepository {

    private final HospitalizationObservationJpaRepository jpaRepository;
    private final HospitalizationObservationJpaMapper mapper;
    private final HospitalizationJpaRepository hospitalizationJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaHospitalizationObservationRepository(
            HospitalizationObservationJpaRepository jpaRepository,
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
        HospitalizationJpaEntity hospitalization = hospitalizationJpaRepository
                .getReferenceById(observation.getHospitalization().id());
        EmployeeJpaEntity createdBy = employeeJpaRepository
                .getReferenceById(observation.getCreatedBy().id());
        HospitalizationObservationJpaEntity saved = jpaRepository
                .save(mapper.toJpa(observation, hospitalization, createdBy));
        return mapper.toDomain(saved, observation.getHospitalization(), observation.getCreatedBy());
    }

    @Override
    public Optional<HospitalizationObservation> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<HospitalizationObservation> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndHospitalization_Company_Id(id, companyId)
                .map(mapper::toDomain);
    }

    /**
     * El orden por id descendente es estable y devuelve primero lo mas reciente,
     * que es lo que la ficha de hospitalizacion muestra arriba.
     */
    @Override
    public PageResult<HospitalizationObservation> findAllByHospitalizationIdAndCompanyId(
            Long hospitalizationId, Long companyId, int page, int pageSize) {
        return Pages.result(
                jpaRepository.findByHospitalizationIdAndHospitalization_Company_Id(
                        hospitalizationId, companyId,
                        Pages.request(page, pageSize, Sort.by(Sort.Direction.DESC, "id"))),
                mapper::toDomain);
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }
}
