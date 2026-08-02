package com.vetsoftware.app.service.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.service.application.command.SearchServicesCommand;
import com.vetsoftware.app.service.application.dto.PageResult;
import com.vetsoftware.app.service.application.port.out.ServiceRepository;
import com.vetsoftware.app.service.domain.Service;
import com.vetsoftware.app.servicecategory.infrastructure.persistence.ServiceCategoryJpaEntity;
import com.vetsoftware.app.servicecategory.infrastructure.persistence.ServiceCategoryJpaRepository;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class JpaServiceRepository implements ServiceRepository {
  private final ServiceJpaRepository jpaRepository;
  private final ServiceJpaMapper mapper;
  private final ServiceCategoryJpaRepository serviceCategoryJpaRepository;
  private final TaxJpaRepository taxJpaRepository;
  private final CompanyJpaRepository companyJpaRepository;

  public JpaServiceRepository(
      ServiceJpaRepository jpaRepository,
      ServiceJpaMapper mapper,
      ServiceCategoryJpaRepository serviceCategoryJpaRepository,
      TaxJpaRepository taxJpaRepository,
      CompanyJpaRepository companyJpaRepository) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
    this.serviceCategoryJpaRepository = serviceCategoryJpaRepository;
    this.taxJpaRepository = taxJpaRepository;
    this.companyJpaRepository = companyJpaRepository;
  }

  @Override
  public Service save(Service service) {
    ServiceCategoryJpaEntity serviceCategory =
        serviceCategoryJpaRepository.getReferenceById(service.getServiceCategory().id());
    CompanyJpaEntity company = companyJpaRepository.getReferenceById(service.getCompany().id());
    TaxJpaEntity tax =
        service.getTax() == null ? null : taxJpaRepository.getReferenceById(service.getTax().id());
    ServiceJpaEntity saved =
        jpaRepository.saveAndFlush(mapper.toJpa(service, serviceCategory, tax, company));
    return mapper.toDomain(
        saved, service.getServiceCategory(), service.getTax(), service.getCompany());
  }

  @Override
  public Optional<Service> findById(Long id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<Service> findByIdAndCompanyId(Long id, Long companyId) {
    return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
  }

  @Override
  public List<Service> findAll() {
    return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Service> findAllByCompanyId(Long companyId) {
    return jpaRepository.findAllByCompanyId(companyId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Service> findAllDisabledByCompanyId(Long companyId) {
    return jpaRepository.findAllDisabledByCompany_Id(companyId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public PageResult<Service> search(SearchServicesCommand command) {
    Specification<ServiceJpaEntity> spec = buildSpec(command);
    PageRequest pageRequest =
        PageRequest.of(
            Math.max(command.page(), 0),
            command.pageSize() <= 0 ? 20 : command.pageSize(),
            Sort.by(Sort.Direction.DESC, "createdDate"));
    Page<ServiceJpaEntity> page = jpaRepository.findAll(spec, pageRequest);
    List<Service> content = page.getContent().stream().map(mapper::toDomain).toList();
    return new PageResult<>(
        content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  private Specification<ServiceJpaEntity> buildSpec(SearchServicesCommand command) {
    return (root, query, cb) -> {
      // fetch-join de los @ManyToOne solo en la query de datos (no en la de count) para evitar N+1
      if (query.getResultType() != Long.class && query.getResultType() != long.class) {
        root.fetch("serviceCategory", JoinType.LEFT);
        root.fetch("tax", JoinType.LEFT);
        root.fetch("company", JoinType.LEFT);
        query.distinct(true);
      }
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("company").get("id"), command.companyId()));
      if (command.name() != null && !command.name().isBlank()) {
        predicates.add(
            cb.like(cb.lower(root.get("name")), "%" + command.name().toLowerCase() + "%"));
      }
      if (command.serviceCategoryId() != null) {
        predicates.add(
            cb.equal(root.get("serviceCategory").get("id"), command.serviceCategoryId()));
      }
      if (command.taxId() != null) {
        predicates.add(cb.equal(root.get("tax").get("id"), command.taxId()));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  @Override
  public void delete(Long id) {
    jpaRepository.deleteById(id);
  }

  @Override
  public int reactivate(Long id, Long companyId) {
    return jpaRepository.reactivate(id, companyId);
  }
}
