package com.vetsoftware.app.surgerytype.infrastructure.persistence;

import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaSurgeryTypeRepository implements SurgeryTypeRepository {
    private final SurgeryTypeJpaRepository jpaRepository;
    private final SurgeryTypeJpaMapper mapper;

    public JpaSurgeryTypeRepository(SurgeryTypeJpaRepository jpaRepository, SurgeryTypeJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public SurgeryType save(SurgeryType surgeryType) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(surgeryType)));
    }

    @Override
    public Optional<SurgeryType> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<SurgeryType> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
