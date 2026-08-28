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

    /**
     * El ARCHIVO de empresas —{@code enabled = false}—, con el mismo alcance que
     * {@link #findAllVisibleTo}. Es la gemela imprescindible de
     * {@link #reactivate(Long)}: sin ella, restaurar una empresa exige conocer su
     * id de antemano, porque ninguna de las tres lecturas de la rodaja puede
     * devolverla.
     *
     * <p>
     * <b>El SQL que hay debajo es NATIVO, y eso cambia quien defiende el
     * tenant.</b> La entidad lleva {@code @SQLRestriction("enabled = true")}, asi
     * que un JPQL —lo que usan {@code findPageByCompanyId} y {@code searchByTerm}—
     * arrastraria la restriccion al {@code WHERE} y no encontraria jamas una fila
     * archivada. El SQL nativo no pasa por la restriccion; tampoco pasa por nada
     * mas. <b>No hay lectura previa que valide la propiedad de ninguna fila</b>,
     * exactamente como en {@code reactivate}: el {@code WHERE} de la consulta es
     * TODA la seguridad que existe en este camino. Por eso el {@code companyId}
     * viaja hasta el SQL y se traduce en un {@code AND id = :companyId}, y no se
     * filtra despues en Java sobre una lista que ya se trajo el archivo entero —
     * filtrar en Java seria correcto en la respuesta y una fuga en el log de
     * consultas, en el plan de ejecucion y en cualquier futuro que reutilice el
     * metodo ancho.
     *
     * @param companyId
     *            la empresa del actor, o {@code null} SOLO para un principal de
     *            plataforma: es la unica señal de «sin acotar» y el unico camino al
     *            archivo completo
     */
    PageResult<Company> findAllDisabledVisibleTo(Long companyId, int page, int pageSize);

    void delete(Long id);

    /**
     * Devuelve al registro una empresa archivada. Es el inverso de
     * {@link #delete(Long)}, que no borra sino que pone {@code enabled = false}.
     *
     * <p>
     * <b>No hay {@code findById} que valga antes</b>: la entidad lleva
     * {@code @SQLRestriction("enabled = true")}, asi que una empresa archivada no
     * la ve ninguna consulta JPA. Por eso el contrato devuelve un conteo en vez de
     * la ficha, y quien llama decide si cero filas es un 404.
     *
     * @return cuantas filas se reactivaron: 1, o 0 si el id no existe o la empresa
     *         ya estaba activa
     */
    int reactivate(Long id);
}
