package com.vetsoftware.app.hospitalizationobservation.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import com.vetsoftware.app.hospitalizationobservation.application.dto.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaHospitalizationObservationRepository
        implements
            HospitalizationObservationRepository {
    private static final int BY_STAY_DEFAULT_PAGE_SIZE = 20;
    private static final int BY_STAY_MAX_PAGE_SIZE = 200;

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

    @Override
    public PageResult<HospitalizationObservation> findAllByHospitalizationIdAndCompanyId(
            Long hospitalizationId, Long companyId, int page, int pageSize) {
        Page<HospitalizationObservationJpaEntity> result = jpaRepository
                .findByHospitalizationIdAndHospitalization_Company_Id(hospitalizationId, companyId,
                        byStayPageRequest(page, pageSize));
        return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    /**
     * Normaliza lo que llega del cliente: una pagina negativa o un tamano desmedido
     * no deben poder volver a pedir el historial entero de la estancia. El orden
     * por id descendente es estable y devuelve primero lo mas reciente, que es lo
     * que la ficha de hospitalizacion muestra arriba.
     */
    private static PageRequest byStayPageRequest(int page, int pageSize) {
        int safeSize = pageSize <= 0
                ? BY_STAY_DEFAULT_PAGE_SIZE
                : Math.min(pageSize, BY_STAY_MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
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
