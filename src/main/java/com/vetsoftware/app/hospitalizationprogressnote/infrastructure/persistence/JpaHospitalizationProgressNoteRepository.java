package com.vetsoftware.app.hospitalizationprogressnote.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaEntity;
import com.vetsoftware.app.hospitalization.infrastructure.persistence.HospitalizationJpaRepository;
import com.vetsoftware.app.hospitalizationprogressnote.application.port.out.HospitalizationProgressNoteRepository;
import com.vetsoftware.app.hospitalizationprogressnote.domain.HospitalizationProgressNote;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaHospitalizationProgressNoteRepository implements HospitalizationProgressNoteRepository {
    private final HospitalizationProgressNoteJpaRepository jpaRepository;
    private final HospitalizationProgressNoteJpaMapper mapper;
    private final HospitalizationJpaRepository hospitalizationJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaHospitalizationProgressNoteRepository(HospitalizationProgressNoteJpaRepository jpaRepository,
                                                   HospitalizationProgressNoteJpaMapper mapper,
                                                   HospitalizationJpaRepository hospitalizationJpaRepository,
                                                   EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.hospitalizationJpaRepository = hospitalizationJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public HospitalizationProgressNote save(HospitalizationProgressNote progressNote) {
        HospitalizationJpaEntity hospitalization =
            hospitalizationJpaRepository.getReferenceById(progressNote.getHospitalization().id());
        EmployeeJpaEntity createdBy =
            employeeJpaRepository.getReferenceById(progressNote.getCreatedBy().id());
        HospitalizationProgressNoteJpaEntity saved =
            jpaRepository.save(mapper.toJpa(progressNote, hospitalization, createdBy));
        return mapper.toDomain(saved, progressNote.getHospitalization(), progressNote.getCreatedBy());
    }

    @Override
    public Optional<HospitalizationProgressNote> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<HospitalizationProgressNote> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndHospitalization_Company_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public List<HospitalizationProgressNote> findAllByHospitalizationId(Long hospitalizationId) {
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
