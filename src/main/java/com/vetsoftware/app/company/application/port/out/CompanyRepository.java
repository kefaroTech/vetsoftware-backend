package com.vetsoftware.app.company.application.port.out;

import com.vetsoftware.app.company.domain.Company;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository {
    Company save(Company company);

    Optional<Company> findById(Long id);

    /**
     * El registro de empresas que puede ver el actor: la suya cuando
     * {@code companyId} viene informado, y el registro completo solo cuando es
     * {@code null} —que es lo que devuelve {@code Authz.currentCompanyIdOrNull()}
     * para un principal de plataforma—.
     *
     * <p>
     * <b>Sin variante ancha a proposito.</b> Mientras el puerto ofrecio un
     * {@code findAll()} pelado, el caso de uso lo llamaba siempre y
     * {@code GET /companies} devolvia el registro mercantil entero —nombre, NIT,
     * direccion, telefono y plan contratado de todos los tenants— a cualquier
     * empleado con {@code company.read}, que es el permiso que necesita para ver la
     * ficha de su propia veterinaria. Sin ese metodo, pedir el listado obliga a
     * declarar el alcance.
     */
    List<Company> findAllVisibleTo(Long companyId);

    void delete(Long id);

    int reactivate(Long id);
}
