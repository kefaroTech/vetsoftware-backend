package com.vetsoftware.app.supplier.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SupplierJpaRepository
    extends JpaRepository<SupplierJpaEntity, Long>, JpaSpecificationExecutor<SupplierJpaEntity> {

  @Override
  @EntityGraph(attributePaths = "company")
  List<SupplierJpaEntity> findAll();

  @Override
  @EntityGraph(attributePaths = "company")
  Optional<SupplierJpaEntity> findById(Long id);

  @EntityGraph(attributePaths = "company")
  List<SupplierJpaEntity> findAllByCompanyId(Long companyId);

  @EntityGraph(attributePaths = "company")
  Optional<SupplierJpaEntity> findByIdAndCompany_Id(Long id, Long companyId);

  // Query nativa: el @SQLRestriction("enabled = true") NO aplica a SQL nativo, así que ésta es la
  // única vía para listar los proveedores PAUSADOS (enabled=false) y poder reactivarlos desde la
  // UI.
  // Las asociaciones se hidratan perezosamente dentro de la transacción de lectura del caso de uso.
  @org.springframework.data.jpa.repository.Query(
      value =
          "SELECT * FROM suppliers WHERE company_id = :companyId AND enabled = false ORDER BY name",
      nativeQuery = true)
  List<SupplierJpaEntity> findAllDisabledByCompany_Id(
      @org.springframework.data.repository.query.Param("companyId") Long companyId);

  @org.springframework.data.jpa.repository.Modifying(
      flushAutomatically = true,
      clearAutomatically = true)
  @org.springframework.transaction.annotation.Transactional
  @org.springframework.data.jpa.repository.Query(
      value = "UPDATE suppliers SET enabled = true WHERE id = :id AND company_id = :companyId",
      nativeQuery = true)
  int reactivate(
      @org.springframework.data.repository.query.Param("id") Long id,
      @org.springframework.data.repository.query.Param("companyId") Long companyId);

  boolean existsByIdAndCompany_Id(Long id, Long companyId);

  // @SQLRestriction("enabled = true") aplica: solo cuenta proveedores ACTIVOS (un name desactivado
  // se reusa).
  boolean existsByCompany_IdAndName(Long companyId, String name);

  boolean existsByCompany_IdAndNameAndIdNot(Long companyId, String name, Long id);
}
