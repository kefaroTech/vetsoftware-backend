package com.vetsoftware.app.branch.infrastructure.persistence;

import com.vetsoftware.app.branch.application.port.out.BranchRepository;
import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaRepository;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaBranchRepository implements BranchRepository {
  private final BranchJpaRepository jpaRepository;
  private final BranchJpaMapper mapper;
  private final CityJpaRepository cityJpaRepository;
  private final CompanyJpaRepository companyJpaRepository;

  public JpaBranchRepository(
      BranchJpaRepository jpaRepository,
      BranchJpaMapper mapper,
      CityJpaRepository cityJpaRepository,
      CompanyJpaRepository companyJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.cityJpaRepository = cityJpaRepository;
    this.companyJpaRepository = companyJpaRepository;
  }

  @Override
  public Branch save(Branch branch) {
    CityJpaEntity city = cityJpaRepository.getReferenceById(branch.getCity().id());
    CompanyJpaEntity company = companyJpaRepository.getReferenceById(branch.getCompany().id());
    BranchJpaEntity saved = jpaRepository.save(mapper.toJpa(branch, city, company));
    return mapper.toDomain(saved, branch.getCity(), branch.getCompany());
  }

  @Override
  public Optional<Branch> findByIdAndCompanyId(Long id, Long companyId) {
    return jpaRepository.findByIdAndCompanyId(id, companyId).map(mapper::toDomain);
  }

  @Override
  public List<Branch> findAllByCompanyId(Long companyId) {
    return jpaRepository.findAllByCompanyId(companyId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public boolean codeExists(Long companyId, String code) {
    return jpaRepository.existsByCompany_IdAndCodeIgnoreCase(companyId, code);
  }

  @Override
  public boolean codeExistsForOther(Long companyId, String code, Long id) {
    return jpaRepository.existsByCompany_IdAndCodeIgnoreCaseAndIdNot(companyId, code, id);
  }

  @Override
  public boolean existsOtherActiveByCompanyId(Long companyId, Long id) {
    return jpaRepository.existsByCompany_IdAndActiveTrueAndIdNot(companyId, id);
  }
}
