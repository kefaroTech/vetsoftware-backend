package com.vetsoftware.app.diagnosticimagingtype.infrastructure.persistence;

import com.vetsoftware.app.diagnosticimagingtype.application.port.out.DiagnosticImagingTypeRepository;
import com.vetsoftware.app.diagnosticimagingtype.domain.DiagnosticImagingType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaDiagnosticImagingTypeRepository implements DiagnosticImagingTypeRepository {
    private final DiagnosticImagingTypeJpaRepository jpaRepository;
    private final DiagnosticImagingTypeJpaMapper mapper;

    public JpaDiagnosticImagingTypeRepository(DiagnosticImagingTypeJpaRepository jpaRepository,
                                              DiagnosticImagingTypeJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public DiagnosticImagingType save(DiagnosticImagingType type) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(type)));
    }

    @Override
    public Optional<DiagnosticImagingType> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<DiagnosticImagingType> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
