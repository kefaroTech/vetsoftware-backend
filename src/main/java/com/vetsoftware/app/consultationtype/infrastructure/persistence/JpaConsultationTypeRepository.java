package com.vetsoftware.app.consultationtype.infrastructure.persistence;

import com.vetsoftware.app.consultationtype.application.port.out.ConsultationTypeRepository;
import com.vetsoftware.app.consultationtype.domain.ConsultationType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaConsultationTypeRepository implements ConsultationTypeRepository {
    private final ConsultationTypeJpaRepository jpaRepository;
    private final ConsultationTypeJpaMapper mapper;

    public JpaConsultationTypeRepository(ConsultationTypeJpaRepository jpaRepository, ConsultationTypeJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ConsultationType save(ConsultationType consultationType) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(consultationType)));
    }

    @Override
    public Optional<ConsultationType> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ConsultationType> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
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
