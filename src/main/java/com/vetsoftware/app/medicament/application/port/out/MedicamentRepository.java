package com.vetsoftware.app.medicament.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.medicament.domain.Medicament;
import java.util.List;
import java.util.Optional;

public interface MedicamentRepository {
    Medicament save(Medicament medicament);

    Optional<Medicament> findById(Long id);

    /**
     * Solo el medicamento PROPIO de la empresa. Es el que valida las escrituras: un
     * general es de la plataforma y ningun tenant puede editarlo, borrarlo ni
     * reactivarlo. Se queda con el nombre canonico justamente porque es el que
     * cualquier {@code Update/Delete/Reactivate} debe usar.
     */
    Optional<Medicament> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Disponible para la empresa: los generales de la plataforma MAS los suyos. Es
     * la vista de lectura y de receta; no autoriza ninguna escritura.
     */
    Optional<Medicament> findAvailableByIdAndCompanyId(Long id, Long companyId);

    /**
     * Catalogo GLOBAL de la plataforma: no filtra por empresa. Pagina porque de
     * otro modo trae la tabla entera; su uso esta restringido a ROLE_SYSTEM.
     */
    PageResult<Medicament> findAll(int page, int pageSize);

    List<Medicament> findAllAvailableForCompany(Long companyId);

    List<Medicament> findAllDisabledForCompany(Long companyId);

    void delete(Long id);

    int reactivate(Long id, Long companyId);
}
