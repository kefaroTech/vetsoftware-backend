package com.vetsoftware.app.vaccinationtype.infrastructure.persistence;

import com.vetsoftware.app.vaccinationtype.application.port.out.VaccinationTypeRepository;
import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaVaccinationTypeRepository implements VaccinationTypeRepository {
    private final VaccinationTypeJpaRepository jpaRepository;
    private final VaccinationTypeJpaMapper mapper;

    public JpaVaccinationTypeRepository(VaccinationTypeJpaRepository jpaRepository, VaccinationTypeJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public VaccinationType save(VaccinationType vaccinationType) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(vaccinationType)));
    }

    @Override
    public Optional<VaccinationType> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<VaccinationType> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
