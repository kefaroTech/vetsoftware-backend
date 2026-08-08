package com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import com.vetsoftware.app.hospitalizationprocedure.application.dto.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaHospitalizationProcedureRepository implements HospitalizationProcedureRepository {

    private static final int BY_STAY_DEFAULT_PAGE_SIZE = 20;
    private static final int BY_STAY_MAX_PAGE_SIZE = 200;
    private final HospitalizationProcedureJpaRepository jpaRepository;
    private final HospitalizationProcedureJpaMapper mapper;
    private final HospitalizationJpaRepository hospitalizationJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaHospitalizationProcedureRepository(
            HospitalizationProcedureJpaRepository jpaRepository,
            HospitalizationProcedureJpaMapper mapper,
            HospitalizationJpaRepository hospitalizationJpaRepository,
            EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.hospitalizationJpaRepository = hospitalizationJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public HospitalizationProcedure save(HospitalizationProcedure procedure) {
        HospitalizationJpaEntity hospitalization = hospitalizationJpaRepository
                .getReferenceById(procedure.getHospitalization().id());
        EmployeeJpaEntity createdBy = employeeJpaRepository
                .getReferenceById(procedure.getCreatedBy().id());
        EmployeeJpaEntity suspensionBy = procedure.getSuspensionBy() == null
                ? null
                : employeeJpaRepository.getReferenceById(procedure.getSuspensionBy().id());
        HospitalizationProcedureJpaEntity saved = jpaRepository
                .save(mapper.toJpa(procedure, hospitalization, createdBy, suspensionBy));
        return mapper.toDomain(saved, procedure.getHospitalization(), procedure.getCreatedBy(),
                procedure.getSuspensionBy());
    }

    @Override
    public Optional<HospitalizationProcedure> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<HospitalizationProcedure> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndHospitalization_Company_Id(id, companyId)
                .map(mapper::toDomain);
    }

    @Override
    public PageResult<HospitalizationProcedure> findAllByHospitalizationId(Long hospitalizationId,
            int page, int pageSize) {
        Page<HospitalizationProcedureJpaEntity> result = jpaRepository
                .findByHospitalizationId(hospitalizationId, byStayPageRequest(page, pageSize));
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
