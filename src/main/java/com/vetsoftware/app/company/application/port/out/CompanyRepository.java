package com.vetsoftware.app.company.application.port.out;

import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

public interface CompanyRepository {
    Company save(Company company);

    Optional<Company> findById(Long id);

    /**
     * El registro de empresas que puede ver el actor, <b>una página cada vez</b>:
     * la suya cuando {@code companyId} viene informado, y el registro completo solo
     * cuando es {@code null} —que es lo que devuelve
     * {@code Authz.currentCompanyIdOrNull()} para un principal de plataforma—.
     *
     * <p>
     * <b>Sin variante ancha a proposito.</b> Mientras el puerto ofrecio un
     * {@code findAll()} pelado, el caso de uso lo llamaba siempre y
     * {@code GET /companies} devolvia el registro mercantil entero —nombre, NIT,
     * direccion, telefono y plan contratado de todos los tenants— a cualquier
     * empleado con {@code company.read}, que es el permiso que necesita para ver la
     * ficha de su propia veterinaria. Sin ese metodo, pedir el listado obliga a
     * declarar el alcance.
     *
     * <p>
     * <b>Y sin variante sin paginar, por la misma razon.</b> Devolver
     * {@code List<Company>} dejaba el tamaño de la respuesta en manos de cuantas
     * empresas hubiera en la tabla: la consola de plataforma pintaba el array
     * entero (VUE-06) y el coste crecia con cada alta sin que nada lo acotara. El
     * tope duro lo pone {@code Pages.MAX_SIZE}, no el cliente.
     */
    PageResult<Company> findAllVisibleTo(Long companyId, int page, int pageSize);

    /**
     * La misma página, filtrada por termino sobre el nombre y el identificador
     * fiscal. El alcance se decide igual que en {@link #findAllVisibleTo}: buscar
     * no ensancha lo que el actor puede ver.
     */
    PageResult<Company> searchVisibleTo(Long companyId, String query, int page, int pageSize);

    void delete(Long id);
}
