package com.vetsoftware.app.hospitalizationprocedure.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationprocedure.application.port.out.HospitalizationProcedureRepository;
import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationProcedure;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import org.springframework.data.domain.Sort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaHospitalizationProcedureRepository implements HospitalizationProcedureRepository {

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

    /**
     * El orden por id descendente es estable y devuelve primero lo mas reciente,
     * que es lo que la ficha de hospitalizacion muestra arriba.
     */
    @Override
    public PageResult<HospitalizationProcedure> findAllByHospitalizationIdAndCompanyId(
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
}
