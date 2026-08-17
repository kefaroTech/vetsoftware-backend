package com.vetsoftware.app.medicament.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.medicament.domain.Medicament;
import java.util.List;
import java.util.Optional;

public interface MedicamentRepository {
    Medicament save(Medicament medicament);

    Optional<Medicament> findById(Long id);

    Optional<Medicament> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Catalogo GLOBAL de la plataforma: no filtra por empresa. Pagina porque de
     * otro modo trae la tabla entera; su uso esta restringido a ROLE_SYSTEM.
     */
    PageResult<Medicament> findAll(int page, int pageSize);

    List<Medicament> findAllAvailableForCompany(Long companyId);

    List<Medicament> findAllDisabledForCompany(Long companyId);

    void delete(Long id);

    int reactivate(Long id);
}
