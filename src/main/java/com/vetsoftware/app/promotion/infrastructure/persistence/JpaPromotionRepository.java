package com.vetsoftware.app.promotion.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.promotion.application.port.out.PromotionRepository;
import com.vetsoftware.app.promotion.domain.Promotion;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaPromotionRepository implements PromotionRepository {
  private final PromotionJpaRepository jpaRepository;
  private final PromotionJpaMapper mapper;
  private final CompanyJpaRepository companyJpaRepository;

  public JpaPromotionRepository(
      PromotionJpaRepository jpaRepository,
      PromotionJpaMapper mapper,
      CompanyJpaRepository companyJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.companyJpaRepository = companyJpaRepository;
  }

  @Override
  public Promotion save(Promotion promotion) {
    CompanyJpaEntity company = companyJpaRepository.getReferenceById(promotion.getCompany().id());
    PromotionJpaEntity saved = jpaRepository.save(mapper.toJpa(promotion, company));
    return mapper.toDomain(saved, promotion.getCompany());
  }

  @Override
  public Optional<Promotion> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Promotion> findByIdAndCompanyId(Long id, Long companyId) {
    return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
  }

  @Override
  public List<Promotion> findAllByCompanyId(Long companyId) {
    return jpaRepository.findAllByCompanyId(companyId).stream().map(mapper::toDomain).toList();
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
